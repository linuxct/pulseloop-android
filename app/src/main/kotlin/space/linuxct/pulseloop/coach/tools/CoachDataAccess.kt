package space.linuxct.pulseloop.coach.tools

import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CoachDataAccess {

    private val isoFmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
    }
    private val dateFmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    fun isoString(epochMs: Long): String = isoFmt.get()!!.format(Date(epochMs))
    fun localDateString(epochMs: Long): String = dateFmt.get()!!.format(Date(epochMs))

    fun parseLocalDate(dateStr: String): Long {
        if (dateStr.equals("today", ignoreCase = true)) return todayMidnightMs()
        return try { dateFmt.get()!!.parse(dateStr)?.time ?: System.currentTimeMillis() }
        catch (_: Exception) { System.currentTimeMillis() }
    }

    fun dayBoundsMs(epochMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to (start + 86_400_000L - 1)
    }

    fun todayMidnightMs(): Long = dayBoundsMs(System.currentTimeMillis()).first

    fun cutoffMs(daysBack: Int): Long = System.currentTimeMillis() - daysBack * 86_400_000L

    fun downsample(samples: List<MeasurementEntity>, max: Int = 200): List<MeasurementEntity> {
        if (samples.size <= max) return samples
        val step = samples.size.toDouble() / max
        return (0 until max).map { samples[(it * step).toInt()] }
    }

    fun stats(values: List<Double>): Map<String, Any> {
        if (values.isEmpty()) return mapOf("count" to 0)
        val sorted = values.sorted()
        return mapOf(
            "count"  to values.size,
            "min"    to sorted.first(),
            "max"    to sorted.last(),
            "avg"    to values.average(),
            "median" to sorted[sorted.size / 2]
        )
    }
}
