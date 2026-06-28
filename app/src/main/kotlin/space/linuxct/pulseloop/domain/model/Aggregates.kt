package space.linuxct.pulseloop.domain.model

import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity

// ─── Measurement aggregates ───────────────────────────────────────────────────

data class MetricSample(val timestamp: Long, val value: Double)
data class DailyMetricPoint(val date: Long, val value: Double)

enum class MetricKey {
    HEART_RATE, SPO2, STEPS, CALORIES, DISTANCE, ACTIVE_MINUTES,
    SLEEP, STRESS, HRV, TEMPERATURE;

    val requiredCapability: WearableCapability? get() = when (this) {
        HEART_RATE     -> WearableCapability.HEART_RATE
        SPO2           -> WearableCapability.SPO2
        STRESS         -> WearableCapability.STRESS
        HRV            -> WearableCapability.HRV
        TEMPERATURE    -> WearableCapability.TEMPERATURE
        STEPS          -> WearableCapability.STEPS
        SLEEP          -> WearableCapability.SLEEP
        else           -> null
    }
}

enum class MetricRange { TWENTY_FOUR_HOURS, SEVEN_DAYS, THIRTY_DAYS, TWELVE_MONTHS }

enum class DataFreshness { LIVE, SYNCED_TODAY, STALE, MISSING, DEMO }

enum class MetricConfidence { HIGH, MEDIUM, LOW, PARTIAL }

data class MetricState(
    val freshness: DataFreshness,
    val confidence: MetricConfidence,
    val source: String?,
    val sampleCount: Int,
    val requiredSamples: Int,
    val lastUpdatedAt: Long?,
    val status: String,
    val zeroIsReal: Boolean = false
)

// ─── Today summary ────────────────────────────────────────────────────────────

data class GoalsSummary(
    val stepsDaily: Int,
    val activeMinutesDaily: Int,
    val sleepHours: Double,
    val exerciseDaysWeekly: Int
)

data class TrendsSummary(
    val steps7d: List<DailyMetricPoint>,
    val calories7d: List<DailyMetricPoint>,
    val distance7d: List<DailyMetricPoint>,
    val hrSamples24h: List<MetricSample>,
    val spo2Samples24h: List<MetricSample>,
    // Sparkline series for the Today tiles (systolic for BP). Empty when the metric is opt-out.
    val bpSysSamples24h: List<MetricSample> = emptyList(),
    val glucoseSamples24h: List<MetricSample> = emptyList()
)

data class TimelineEvent(
    val title: String,
    val detail: String,
    val timestamp: Long,
    val metric: String
)

enum class CalibrationStatus { CALIBRATING, READY }

data class CalibrationState(
    val isCalibrating: Boolean,
    val day: Int,
    val totalDays: Int,
    val startedAt: Long?,
    val reason: String
)

data class TodaySummary(
    val date: Long,
    val steps: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
    val activeMinutes: Int?,
    val activeMinutesSource: String,
    val latestHeartRate: Double?,
    val latestSpO2: Double?,
    val restingHeartRateEstimate: Double?,
    val peakHeartRateToday: Double?,
    // Jring combined-measurement spot metrics (calibration offsets already applied). Null when unmeasured.
    val latestBloodPressureSystolic: Double? = null,
    val latestBloodPressureDiastolic: Double? = null,
    val latestBloodSugar: Double? = null,
    val sleep: SleepSummary?,
    val batteryPercent: Int,
    val deviceState: RingConnectionState,
    val trends: TrendsSummary,
    val timeline: List<TimelineEvent>,
    val metricStates: Map<MetricKey, MetricState>,
    val calibration: CalibrationState,
    val goals: GoalsSummary,
    val isDemo: Boolean
)

// ─── Sleep aggregates ─────────────────────────────────────────────────────────

data class SleepSummary(
    val session: SleepSessionEntity,
    val blocks: List<SleepStageBlockEntity>,
    val lightMinutes: Int,
    val deepMinutes: Int,
    val awakeMinutes: Int,
    val remMinutes: Int = 0
) {
    val totalMinutes: Int get() {
        val fromBlocks = lightMinutes + deepMinutes + awakeMinutes + remMinutes
        return if (fromBlocks > 0) fromBlocks else ((session.endAt - session.startAt) / 60_000).toInt()
    }
}

data class SleepRangeSummary(
    val range: SleepRangeKey,
    val start: Long,
    val end: Long,
    val expectedNights: Int,
    val sessions: List<SleepSummary>
)

data class SleepBar(
    val label: String,
    val durationMin: Int?,
    val score: Int?,
    val present: Boolean
)

// ─── Activity session aggregates ──────────────────────────────────────────────

data class ActivitySessionSummary(
    val session: ActivitySessionEntity,
    val durationSeconds: Int,
    val distanceMeters: Double?,
    val calories: Double,
    val averageHeartRate: Double?,
    val minHeartRate: Double?,
    val maxHeartRate: Double?,
    val averageSpO2: Double?,
    val latestSpO2: Double?,
    val heartRateSampleCount: Int,
    val spo2SampleCount: Int
)

data class ActiveMinutesResult(val minutes: Int, val source: String)

// ─── Vitals history aggregates ────────────────────────────────────────────────

enum class VitalsRangeKey { DAY, WEEK, MONTH, YEAR }

data class VitalsBar(
    val label: String,
    val avgValue: Double?,
    val minValue: Double?,
    val maxValue: Double?,
    val present: Boolean
)

// ─── Activity history aggregates ─────────────────────────────────────────────

enum class ActivityRangeKey { DAY, WEEK, MONTH, YEAR }

data class ActivityBar(
    val label: String,
    val steps: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
    val activeMinutes: Int?,
    val present: Boolean
)
