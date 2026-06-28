package space.linuxct.pulseloop.coach.tools

import org.json.JSONObject
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CoachDataAccess {

    /** Upper bound for any relative look-back. Set high enough that the only real limit
     *  is how much history is actually stored on the device (~10 years). */
    const val MAX_RANGE_DAYS = 3660

    /** A resolved [startMs, endMs] window plus a human-readable label for tool output. */
    data class CoachRange(val startMs: Long, val endMs: Long, val label: String)

    /**
     * Resolves a time window from tool arguments, supporting BOTH:
     *  - an absolute range via `start_date` / `end_date` (yyyy-MM-dd, end inclusive), and
     *  - a relative look-back via [relKey] (e.g. "days" / "nights").
     *
     * Absolute wins when `start_date` is present. A missing `end_date` means "up to now".
     * This is what lets the coach reach ANY historic window, not just the last N days.
     */
    fun resolveRange(o: JSONObject, relKey: String, defaultN: Int): CoachRange {
        val hasStart = !o.isNull("start_date") && o.optString("start_date").isNotBlank()
        if (hasStart) {
            val start = dayBoundsMs(parseLocalDate(o.optString("start_date"))).first
            val hasEnd = !o.isNull("end_date") && o.optString("end_date").isNotBlank()
            val end = if (hasEnd) dayBoundsMs(parseLocalDate(o.optString("end_date"))).second
                      else System.currentTimeMillis()
            val safeEnd = maxOf(end, start)
            return CoachRange(start, safeEnd, "${localDateString(start)} → ${localDateString(safeEnd)}")
        }
        val n = (if (!o.isNull(relKey)) o.optInt(relKey, defaultN) else defaultN)
            .coerceIn(1, MAX_RANGE_DAYS)
        return CoachRange(cutoffMs(n), System.currentTimeMillis(), "last $n $relKey")
    }

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

    fun downsample(samples: List<MeasurementEntity>, max: Int = 200): List<MeasurementEntity> =
        downsampleList(samples, max)

    /** Uniform-stride subsample of any list down to at most [max] elements. */
    fun <T> downsampleList(items: List<T>, max: Int = 200): List<T> {
        if (items.size <= max) return items
        val step = items.size.toDouble() / max
        return (0 until max).map { items[(it * step).toInt()] }
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
