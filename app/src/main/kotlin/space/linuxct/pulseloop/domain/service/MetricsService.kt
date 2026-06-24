package space.linuxct.pulseloop.domain.service

import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.domain.model.CalibrationState
import space.linuxct.pulseloop.domain.model.DailyMetricPoint
import space.linuxct.pulseloop.domain.model.DataFreshness
import space.linuxct.pulseloop.domain.model.GoalsSummary
import space.linuxct.pulseloop.domain.model.MetricConfidence
import space.linuxct.pulseloop.domain.model.MetricKey
import space.linuxct.pulseloop.domain.model.MetricRange
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.domain.model.MetricState
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.SleepSummary
import space.linuxct.pulseloop.domain.model.TimelineEvent
import space.linuxct.pulseloop.domain.model.TodaySummary
import space.linuxct.pulseloop.domain.model.TrendsSummary
import space.linuxct.pulseloop.domain.model.WearableCapability
import java.util.Calendar
import java.util.concurrent.TimeUnit

object MetricsService {

    private const val CALIBRATION_DAYS = 7
    private const val MIN_TREND_DAYS   = 3

    // ─── Public API ──────────────────────────────────────────────────────────

    fun buildTodaySummary(
        activityRows: List<ActivityDailyEntity>,
        measurements: List<MeasurementEntity>,
        device: DeviceEntity?,
        sleepSummary: SleepSummary?,
        sleepBlocks: List<SleepStageBlockEntity>,
        goals: UserGoalEntity?,
        profile: UserProfileEntity? = null
    ): TodaySummary {
        val todayMs = todayMidnightMs()
        val today = activityRows.filter { it.date <= todayMs }.maxByOrNull { it.date }
        val hrRows   = measurements.filter { it.kindRaw == "hr" }.sortedBy { it.timestamp }
        val spo2Rows = measurements.filter { it.kindRaw == "spo2" }.sortedBy { it.timestamp }
        val hrSamples   = samplesSinceCutoff(hrRows,   MetricRange.TWENTY_FOUR_HOURS)
        val spo2Samples = samplesSinceCutoff(spo2Rows, MetricRange.TWENTY_FOUR_HOURS)
        val latestHR   = hrRows.lastOrNull()
        val latestSpO2 = spo2Rows.lastOrNull()
        val hrFreshness   = freshness(latestHR?.timestamp)
        val spo2Freshness = freshness(latestSpO2?.timestamp)
        val calibration   = calibrationState(device, activityRows, measurements)
        val aligned = alignedWeekActivity(activityRows, todayMs)
        val trends = TrendsSummary(
            steps7d    = aligned.map { DailyMetricPoint(it.date, it.steps.toDouble()) },
            calories7d = aligned.map { DailyMetricPoint(it.date, it.calories) },
            distance7d = aligned.map { DailyMetricPoint(it.date, it.distanceMeters) },
            hrSamples24h   = hrSamples,
            spo2Samples24h = spo2Samples
        )
        val enrichedSleep = sleepSummary?.let { s ->
            if (sleepBlocks.isEmpty()) s
            else {
                val light = sleepBlocks.filter { it.stageRaw == "light" }.sumOf { it.durationMinutes }
                val deep  = sleepBlocks.filter { it.stageRaw == "deep" }.sumOf { it.durationMinutes }
                val awake = sleepBlocks.filter { it.stageRaw == "awake" }.sumOf { it.durationMinutes }
                val rem   = sleepBlocks.filter { it.stageRaw == "rem" }.sumOf { it.durationMinutes }
                s.copy(lightMinutes = light, deepMinutes = deep, awakeMinutes = awake, remMinutes = rem, blocks = sleepBlocks)
            }
        }
        val metricStates = buildMetricStates(today, enrichedSleep, latestHR, latestSpO2, hrFreshness, spo2Freshness, activityRows, calibration)
        val goalsSummary = goals?.let {
            GoalsSummary(stepsDaily = it.dailySteps, activeMinutesDaily = it.activeMinutes, sleepHours = it.sleepMinutes / 60.0, exerciseDaysWeekly = 4)
        } ?: GoalsSummary(8000, 60, 7.5, 4)
        val todayCalories = computeCalories(today, profile)

        return TodaySummary(
            date           = today?.date ?: todayMs,
            steps          = today?.steps,
            calories       = todayCalories,
            distanceMeters = today?.distanceMeters,
            activeMinutes  = today?.activeMinutes,
            activeMinutesSource = today?.source ?: "none",
            latestHeartRate  = if (isFresh(hrFreshness))   latestHR?.value   else null,
            latestSpO2       = if (isFresh(spo2Freshness)) latestSpO2?.value else null,
            restingHeartRateEstimate = restingHeartRate(hrSamples),
            peakHeartRateToday       = hrSamples.maxOfOrNull { it.value },
            sleep          = enrichedSleep,
            batteryPercent = device?.batteryLevel ?: 0,
            deviceState    = device?.stateRaw?.let { r -> RingConnectionState.entries.firstOrNull { it.rawValue == r } } ?: RingConnectionState.IDLE,
            trends         = trends,
            timeline       = buildTimeline(device, today, hrSamples, spo2Samples, enrichedSleep),
            metricStates   = metricStates,
            calibration    = calibration,
            goals          = goalsSummary,
            isDemo         = false
        )
    }

    fun metricRange(metric: MetricKey, range: MetricRange, measurements: List<MeasurementEntity>, activityRows: List<ActivityDailyEntity>): List<MetricSample> =
        when (metric) {
            MetricKey.HEART_RATE    -> samplesSinceCutoff(measurements.filter { it.kindRaw == "hr" }, range)
            MetricKey.SPO2          -> samplesSinceCutoff(measurements.filter { it.kindRaw == "spo2" }, range)
            MetricKey.STRESS        -> samplesSinceCutoff(measurements.filter { it.kindRaw == "stress" }, range)
            MetricKey.HRV           -> samplesSinceCutoff(measurements.filter { it.kindRaw == "hrv" }, range)
            MetricKey.TEMPERATURE   -> samplesSinceCutoff(measurements.filter { it.kindRaw == "temp" }, range)
            else                    -> activityMetricSamples(metric, range, activityRows)
        }

    fun deviceCapabilities(device: DeviceEntity?): Set<WearableCapability> {
        val csv = device?.capabilitiesRaw ?: ""
        if (csv.isBlank()) return setOf(WearableCapability.HEART_RATE, WearableCapability.SPO2, WearableCapability.STEPS, WearableCapability.SLEEP, WearableCapability.BATTERY)
        return csv.split(",").mapNotNull { raw -> WearableCapability.entries.firstOrNull { it.rawValue == raw.trim() } }.toSet()
    }

    fun supports(metric: MetricKey, device: DeviceEntity?): Boolean {
        val required = metric.requiredCapability ?: return true
        return deviceCapabilities(device).contains(required)
    }

    // ─── Internals ───────────────────────────────────────────────────────────

    private fun samplesSinceCutoff(rows: List<MeasurementEntity>, range: MetricRange): List<MetricSample> {
        val cutoffMs = cutoffMs(range)
        return rows.filter { it.timestamp >= cutoffMs }.sortedBy { it.timestamp }.map { MetricSample(it.timestamp, it.value) }
    }

    private fun activityMetricSamples(metric: MetricKey, range: MetricRange, rows: List<ActivityDailyEntity>): List<MetricSample> {
        val cutoffMs = cutoffMs(range)
        val filtered = rows.filter { it.date >= cutoffMs }
        if (range == MetricRange.TWELVE_MONTHS) {
            val cal = Calendar.getInstance()
            val grouped = filtered.groupBy { r ->
                cal.timeInMillis = r.date
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            }
            return grouped.mapNotNull { (_, group) ->
                val firstDate = group.minOf { it.date }
                val value = group.sumOf { activityValue(it, metric) }
                MetricSample(firstDate, value)
            }.sortedBy { it.timestamp }
        }
        return filtered.map { MetricSample(it.date, activityValue(it, metric)) }
    }

    private fun activityValue(row: ActivityDailyEntity, metric: MetricKey): Double = when (metric) {
        MetricKey.STEPS          -> row.steps.toDouble()
        MetricKey.CALORIES       -> row.calories
        MetricKey.DISTANCE       -> row.distanceMeters
        MetricKey.ACTIVE_MINUTES -> row.activeMinutes.toDouble()
        else                     -> 0.0
    }

    private fun cutoffMs(range: MetricRange): Long {
        val now = System.currentTimeMillis()
        return when (range) {
            MetricRange.TWENTY_FOUR_HOURS -> now - TimeUnit.HOURS.toMillis(24)
            MetricRange.SEVEN_DAYS        -> now - TimeUnit.DAYS.toMillis(7)
            MetricRange.THIRTY_DAYS       -> now - TimeUnit.DAYS.toMillis(30)
            MetricRange.TWELVE_MONTHS     -> now - TimeUnit.DAYS.toMillis(365)
        }
    }

    private fun alignedWeekActivity(rows: List<ActivityDailyEntity>, anchorMs: Long): List<ActivityDailyEntity> {
        val cal = Calendar.getInstance().apply { timeInMillis = anchorMs; firstDayOfWeek = Calendar.MONDAY }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = (dow + 5) % 7
        val weekStartMs = anchorMs - daysSinceMonday * 86_400_000L
        val byDay = rows.associateBy { dayMidnightMs(it.date) }
        return (0..6).map { offset ->
            val dayMs = weekStartMs + offset * 86_400_000L
            byDay[dayMs] ?: ActivityDailyEntity(id = "", date = dayMs, steps = 0, calories = 0.0, distanceMeters = 0.0, activeMinutes = 0, source = "none", updatedAt = dayMs)
        }
    }

    private fun calibrationState(device: DeviceEntity?, activityRows: List<ActivityDailyEntity>, measurements: List<MeasurementEntity>): CalibrationState {
        val candidates = mutableListOf<Long>()
        device?.lastSeenAt?.let { candidates.add(it) }
        candidates.addAll(activityRows.map { it.updatedAt })
        candidates.addAll(measurements.map { it.timestamp })
        val startedMs = candidates.minOrNull()
        val day = if (startedMs != null) {
            val elapsed = ((todayMidnightMs() - dayMidnightMs(startedMs)) / 86_400_000L).toInt()
            maxOf(1, minOf(CALIBRATION_DAYS, elapsed + 1))
        } else 1
        return CalibrationState(
            isCalibrating = day < CALIBRATION_DAYS,
            day = day, totalDays = CALIBRATION_DAYS, startedAt = startedMs,
            reason = if (day < CALIBRATION_DAYS) "Learning your baseline" else "Baseline ready"
        )
    }

    private fun freshness(lastMs: Long?): DataFreshness {
        if (lastMs == null) return DataFreshness.MISSING
        val todayStart = todayMidnightMs()
        return if (lastMs >= todayStart) DataFreshness.SYNCED_TODAY else DataFreshness.STALE
    }

    private fun isFresh(f: DataFreshness) = f in listOf(DataFreshness.LIVE, DataFreshness.SYNCED_TODAY, DataFreshness.DEMO)

    private fun restingHeartRate(samples: List<MetricSample>): Double? =
        samples.map { it.value }.filter { it <= 72 }.minOrNull()

    private fun buildMetricStates(
        today: ActivityDailyEntity?,
        sleep: SleepSummary?,
        latestHR: MeasurementEntity?,
        latestSpO2: MeasurementEntity?,
        hrFreshness: DataFreshness,
        spo2Freshness: DataFreshness,
        activityRows: List<ActivityDailyEntity>,
        calibration: CalibrationState
    ): Map<MetricKey, MetricState> {
        val activityFreshness = freshness(today?.updatedAt)
        val activityCount = activityRows.size
        fun activityStatus(label: String) = when {
            today == null         -> "No data yet"
            calibration.isCalibrating -> "Baseline learning"
            else                  -> "$label synced"
        }
        fun readingStatus(exists: Boolean) = when {
            exists && calibration.isCalibrating -> "Baseline learning"
            exists                              -> "Latest reading"
            else                                -> "No reading yet"
        }
        return mapOf(
            MetricKey.STEPS    to MetricState(activityFreshness, if (today == null) MetricConfidence.PARTIAL else MetricConfidence.HIGH,   today?.source, activityCount, MIN_TREND_DAYS, today?.updatedAt, activityStatus("Steps")),
            MetricKey.CALORIES to MetricState(activityFreshness, if (today == null) MetricConfidence.PARTIAL else MetricConfidence.LOW,    today?.source, activityCount, MIN_TREND_DAYS, today?.updatedAt, activityStatus("Calories")),
            MetricKey.DISTANCE to MetricState(activityFreshness, if (today == null) MetricConfidence.PARTIAL else MetricConfidence.MEDIUM, today?.source, activityCount, MIN_TREND_DAYS, today?.updatedAt, activityStatus("Distance")),
            MetricKey.ACTIVE_MINUTES to MetricState(activityFreshness, if (today == null) MetricConfidence.PARTIAL else MetricConfidence.MEDIUM, today?.source, activityCount, MIN_TREND_DAYS, today?.updatedAt, activityStatus("Active minutes")),
            MetricKey.HEART_RATE to MetricState(hrFreshness,   if (latestHR == null)   MetricConfidence.PARTIAL else MetricConfidence.HIGH, latestHR?.sourceRaw,   if (latestHR == null) 0 else 1,   1, latestHR?.timestamp,   readingStatus(latestHR != null)),
            MetricKey.SPO2       to MetricState(spo2Freshness, if (latestSpO2 == null) MetricConfidence.PARTIAL else MetricConfidence.HIGH, latestSpO2?.sourceRaw, if (latestSpO2 == null) 0 else 1, 1, latestSpO2?.timestamp, readingStatus(latestSpO2 != null)),
            MetricKey.SLEEP      to MetricState(freshness(sleep?.session?.syncedAt), if (sleep == null) MetricConfidence.PARTIAL else MetricConfidence.MEDIUM, if (sleep?.session?.syncedAt == null) null else "sync", if (sleep == null) 0 else 1, 1, sleep?.session?.syncedAt, if (sleep == null) "No sleep data" else if (calibration.isCalibrating) "Baseline learning" else "Sleep synced")
        )
    }

    private fun buildTimeline(
        device: DeviceEntity?,
        today: ActivityDailyEntity?,
        hrSamples: List<MetricSample>,
        spo2Samples: List<MetricSample>,
        sleep: SleepSummary?
    ): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        device?.lastSeenAt?.let { events.add(TimelineEvent("Sync complete", "Ring data updated", it, "sync")) }
        hrSamples.lastOrNull()?.let { events.add(TimelineEvent("Heart rate", "${it.value.toInt()} bpm", it.timestamp, "hr")) }
        spo2Samples.lastOrNull()?.let { events.add(TimelineEvent("SpO2", "${it.value.toInt()} %", it.timestamp, "spo2")) }
        if (today != null && today.activeMinutes > 0) {
            events.add(TimelineEvent("Activity sync", "${minOf(today.activeMinutes, 22)} min active", today.updatedAt, "activity"))
        }
        sleep?.let { s ->
            val durationMin = ((s.session.endAt - s.session.startAt) / 60_000).toInt()
            events.add(TimelineEvent("Sleep synced", "${durationMin / 60}h ${durationMin % 60}m", s.session.endAt, "sleep"))
        }
        return events.sortedByDescending { it.timestamp }.take(8)
    }

    /**
     * Returns the best available calorie estimate for [row].
     * Uses ring-reported calories only when they come from a non-history (live) source.
     * Otherwise falls back to Mifflin-St Jeor BMR prorated by time elapsed in the day.
     *
     * [isToday] controls the time fraction: true = fraction of day elapsed so far,
     * false = 1.0 (full completed day, used for historical rows).
     */
    fun computeCalories(row: ActivityDailyEntity?, profile: UserProfileEntity?, isToday: Boolean = true): Double? {
        // If the ring has an activity row for this day, its value is authoritative — including 0.
        // BMR fallback only applies when we have no ring data at all.
        if (row != null) return row.calories
        // No ring data — fall back to BMR estimate if profile is complete
        val w = profile?.weightKg ?: return null
        val h = profile.heightCm?.toDouble() ?: return null
        val a = profile.age?.toDouble() ?: return null
        val bmr = when (profile.biologicalSex?.lowercase()) {
            "female" -> 10.0 * w + 6.25 * h - 5.0 * a - 161.0
            else     -> 10.0 * w + 6.25 * h - 5.0 * a + 5.0
        }
        val fractionOfDay = if (isToday) {
            val cal = Calendar.getInstance()
            (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) / 1440.0
        } else {
            1.0
        }
        return bmr * fractionOfDay
    }

    /** Returns "ring" if the row uses ring-reported calories, "bmr_estimate" otherwise. */
    fun caloriesSource(row: ActivityDailyEntity?): String =
        if (row != null) "ring" else "bmr_estimate"

    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayMidnightMs(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
