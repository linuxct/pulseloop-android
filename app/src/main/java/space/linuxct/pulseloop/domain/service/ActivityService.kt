package space.linuxct.pulseloop.domain.service

import space.linuxct.pulseloop.core.util.HaversineUtil
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.domain.model.ActiveMinutesResult
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.domain.model.ActivitySessionSummary
import space.linuxct.pulseloop.domain.model.MeasurementSource
import java.util.Calendar

object ActivityService {

    const val RING_HISTORY_SOURCE = "ring_history"

    private const val DEFAULT_RESTING_HR   = 60.0
    private const val ABSOLUTE_FLOOR_BPM   = 100.0
    private const val RESTING_OFFSET_BPM   = 30.0
    private const val LIVE_DENSITY_THRESHOLD = 5
    private const val MINUTES_PER_BUCKET   = 30

    // ─── Active minutes ───────────────────────────────────────────────────────

    fun computeActiveMinutes(
        dateMs: Long,
        hrMeasurements: List<MeasurementEntity>,
        allHrForResting: List<MeasurementEntity>
    ): ActiveMinutesResult {
        val daySamples = hrMeasurements.filter { isSameDay(it.timestamp, dateMs) }
        if (daySamples.isEmpty()) return ActiveMinutesResult(0, "none")
        val resting = restingHeartRate(allHrForResting)
        val threshold = maxOf(ABSOLUTE_FLOOR_BPM, resting + RESTING_OFFSET_BPM)
        val grouped = daySamples.groupBy { bucketKey(it.timestamp) }
        var total = 0
        var source = "hr_buckets"
        for (bucketSamples in grouped.values) {
            val result = credit(bucketSamples, threshold)
            total += result.minutes
            if (result.source == "hr_live") source = "hr_live"
        }
        return ActiveMinutesResult(minOf(total, 1440), source)
    }

    // ─── Session summary ──────────────────────────────────────────────────────

    fun buildSessionSummary(
        session: ActivitySessionEntity,
        samples: List<ActivitySampleEntity>,
        gpsPoints: List<ActivityGpsPointEntity>,
        endedAt: Long = System.currentTimeMillis()
    ): ActivitySessionSummary {
        val hr    = samples.filter { it.kindRaw == "hr" && it.value > 0 }.map { it.value }
        val spo2  = samples.filter { it.kindRaw == "spo2" && it.value > 0 }.sortedBy { it.timestamp }.map { it.value }
        val acceptedPoints = gpsPoints.filter { it.accepted }.sortedBy { it.timestamp }
        val distance = if (acceptedPoints.size >= 2) {
            HaversineUtil.totalDistanceMeters(acceptedPoints.map { Pair(it.latitude, it.longitude) })
        } else null
        val elapsedSeconds = maxOf(0L, endedAt - session.startedAt - session.elapsedPausedMs) / 1000
        val calories = distance?.let { maxOf(0.0, elapsedSeconds / 60.0 * 8.0) }
            ?: maxOf(0.0, elapsedSeconds / 60.0 * 8.0)

        return ActivitySessionSummary(
            session              = session,
            durationSeconds      = elapsedSeconds.toInt(),
            distanceMeters       = distance,
            calories             = calories,
            averageHeartRate     = hr.takeIf { it.isNotEmpty() }?.average(),
            minHeartRate         = hr.minOrNull(),
            maxHeartRate         = hr.maxOrNull(),
            averageSpO2          = spo2.takeIf { it.isNotEmpty() }?.average(),
            latestSpO2           = spo2.lastOrNull(),
            heartRateSampleCount = hr.size,
            spo2SampleCount      = spo2.size
        )
    }

    // ─── Internals ────────────────────────────────────────────────────────────

    private fun restingHeartRate(samples: List<MeasurementEntity>): Double {
        val values = samples.map { it.value }.filter { it > 0 }.sorted()
        if (values.size < 20) return DEFAULT_RESTING_HR
        val index = maxOf(0, (0.10 * values.size).toInt() - 1)
        return values[index]
    }

    private fun bucketKey(timestampMs: Long): String {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = timestampMs }
        val minute = if (cal.get(Calendar.MINUTE) < 30) 0 else 30
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}-${cal.get(Calendar.HOUR_OF_DAY)}-$minute"
    }

    private fun credit(samples: List<MeasurementEntity>, threshold: Double): ActiveMinutesResult {
        val liveSamples = samples.filter { it.sourceRaw == MeasurementSource.LIVE.rawValue }
        if (liveSamples.size >= LIVE_DENSITY_THRESHOLD) {
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            val perMinute = liveSamples.groupBy { s -> cal.also { it.timeInMillis = s.timestamp }.get(Calendar.MINUTE) }
            val credited = perMinute.values.count { bucket ->
                val mean = bucket.sumOf { it.value } / bucket.size
                mean >= threshold
            }
            return ActiveMinutesResult(credited, "hr_live")
        }
        val mean = samples.sumOf { it.value } / samples.size
        return ActiveMinutesResult(if (mean >= threshold) MINUTES_PER_BUCKET else 0, "hr_buckets")
    }

    private fun isSameDay(tsMs: Long, dayMs: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = tsMs }
        val day = Calendar.getInstance().apply { timeInMillis = dayMs }
        return cal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
               cal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    }
}
