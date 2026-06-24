package space.linuxct.pulseloop.coach.tools

import org.json.JSONArray
import org.json.JSONObject
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.cutoffMs
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.isoString
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.stats
import kotlin.math.abs
import kotlin.math.sqrt

object AnalysisTools {

    fun build(): List<CoachTool> = listOf(
        analyzeTrend(),
        comparePeriods(),
        computeCorrelation(),
        detectOutliers(),
        summarizeDistribution()
    )

    // ── Specs ──────────────────────────────────────────────────────────────

    private const val METRIC_ENUM =
        """["hr","spo2","hrv","stress","temp","steps","calories","distance","active_minutes"]"""

    private const val SPEC_ANALYZE_TREND =
        """{"type":"function","name":"analyze_trend","description":"Computes the linear trend (slope, direction, and strength) for a metric over the last N days. Useful for determining whether a metric is improving, declining, or stable.","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":$METRIC_ENUM},"days":{"type":"integer","description":"Number of days to analyse (3–90)"}},"required":["metric","days"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_COMPARE_PERIODS =
        """{"type":"function","name":"compare_periods","description":"Compares the average of a metric between two consecutive time periods of equal length. Returns the absolute and relative change.","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":$METRIC_ENUM},"period_days":{"type":"integer","description":"Length in days of each period (1–45). The most recent period is compared against the period immediately before it."}},"required":["metric","period_days"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_COMPUTE_CORRELATION =
        """{"type":"function","name":"compute_correlation","description":"Calculates the Pearson correlation between two daily metrics over the last N days.","parameters":{"type":"object","properties":{"metric_a":{"type":"string","enum":$METRIC_ENUM},"metric_b":{"type":"string","enum":$METRIC_ENUM},"days":{"type":"integer","description":"Number of days (7–90)"}},"required":["metric_a","metric_b","days"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_DETECT_OUTLIERS =
        """{"type":"function","name":"detect_outliers","description":"Finds readings that are unusually high or low (beyond a standard-deviation threshold) for a metric.","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":$METRIC_ENUM},"days":{"type":"integer","description":"Number of days to search (1–90)"},"sigma":{"type":"number","description":"Standard deviations from mean to flag as outlier (1.5–4). Use 2.0 as a sensible default."}},"required":["metric","days","sigma"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_SUMMARIZE_DISTRIBUTION =
        """{"type":"function","name":"summarize_distribution","description":"Returns percentile-based distribution statistics for a metric over the last N days.","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":$METRIC_ENUM},"days":{"type":"integer","description":"Number of days (1–90)"}},"required":["metric","days"],"additionalProperties":false},"strict":true}"""

    // ── Handlers ───────────────────────────────────────────────────────────

    private suspend fun metricValues(metric: String, daysBack: Int, ctx: ToolContext): List<Double> {
        val cutoff = cutoffMs(daysBack)
        return when (metric) {
            "hr"             -> ctx.measurementDao.getByKindSince("hr", cutoff).map { it.value }
            "spo2"           -> ctx.measurementDao.getByKindSince("spo2", cutoff).map { it.value }
            "hrv"            -> ctx.measurementDao.getByKindSince("hrv", cutoff).map { it.value }
            "stress"         -> ctx.measurementDao.getByKindSince("stress", cutoff).map { it.value }
            "temp"           -> ctx.measurementDao.getByKindSince("temp", cutoff).map { it.value }
            "steps"          -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.steps.toDouble() }
            "calories"       -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.calories }
            "distance"       -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.distanceMeters }
            "active_minutes" -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.activeMinutes.toDouble() }
            else -> emptyList()
        }
    }

    private suspend fun metricSeries(metric: String, daysBack: Int, ctx: ToolContext): List<Pair<Long, Double>> {
        val cutoff = cutoffMs(daysBack)
        return when (metric) {
            "hr"             -> ctx.measurementDao.getByKindSince("hr", cutoff).sortedBy { it.timestamp }.map { it.timestamp to it.value }
            "spo2"           -> ctx.measurementDao.getByKindSince("spo2", cutoff).sortedBy { it.timestamp }.map { it.timestamp to it.value }
            "hrv"            -> ctx.measurementDao.getByKindSince("hrv", cutoff).sortedBy { it.timestamp }.map { it.timestamp to it.value }
            "stress"         -> ctx.measurementDao.getByKindSince("stress", cutoff).sortedBy { it.timestamp }.map { it.timestamp to it.value }
            "temp"           -> ctx.measurementDao.getByKindSince("temp", cutoff).sortedBy { it.timestamp }.map { it.timestamp to it.value }
            "steps"          -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.date to it.steps.toDouble() }
            "calories"       -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.date to it.calories }
            "distance"       -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.date to it.distanceMeters }
            "active_minutes" -> ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }.map { it.date to it.activeMinutes.toDouble() }
            else -> emptyList()
        }
    }

    private fun linearSlope(series: List<Double>): Double {
        if (series.size < 2) return 0.0
        val n = series.size
        val xMean = (n - 1) / 2.0
        val yMean = series.average()
        var num = 0.0; var den = 0.0
        series.forEachIndexed { i, y ->
            num += (i - xMean) * (y - yMean)
            den += (i - xMean) * (i - xMean)
        }
        return if (den == 0.0) 0.0 else num / den
    }

    private fun pearsonCorr(xs: List<Double>, ys: List<Double>): Double {
        val n = minOf(xs.size, ys.size)
        if (n < 3) return 0.0
        val xm = xs.take(n).average(); val ym = ys.take(n).average()
        var num = 0.0; var dx = 0.0; var dy = 0.0
        for (i in 0 until n) {
            val ex = xs[i] - xm; val ey = ys[i] - ym
            num += ex * ey; dx += ex * ex; dy += ey * ey
        }
        val denom = sqrt(dx * dy)
        return if (denom == 0.0) 0.0 else num / denom
    }

    private fun analyzeTrend() = CoachTool(
        name = "analyze_trend",
        specJson = SPEC_ANALYZE_TREND,
        run = { args, ctx ->
            val o      = JSONObject(args)
            val metric = o.optString("metric", "hr")
            val days   = o.optInt("days", 7).coerceIn(3, 90)
            val series = metricSeries(metric, days, ctx)
            val values = series.map { it.second }

            val slope = linearSlope(values)
            val direction = when {
                abs(slope) < 0.01    -> "stable"
                slope > 0            -> "increasing"
                else                 -> "decreasing"
            }
            val statsMap = stats(values)

            val obj = JSONObject().apply {
                put("metric",    metric)
                put("days",      days)
                put("samples",   values.size)
                put("slope_per_day", slope)
                put("direction", direction)
                put("first_value", values.firstOrNull())
                put("last_value",  values.lastOrNull())
                put("stats",     JSONObject(statsMap))
            }
            ToolResult.success(obj)
        }
    )

    private fun comparePeriods() = CoachTool(
        name = "compare_periods",
        specJson = SPEC_COMPARE_PERIODS,
        run = { args, ctx ->
            val o      = JSONObject(args)
            val metric = o.optString("metric", "steps")
            val pDays  = o.optInt("period_days", 7).coerceIn(1, 45)

            val fullSeries   = metricSeries(metric, pDays * 2, ctx)
            val half         = fullSeries.size / 2
            val priorVals    = fullSeries.take(half).map { it.second }
            val recentVals   = fullSeries.drop(half).map { it.second }

            val recentAvg = if (recentVals.isEmpty()) 0.0 else recentVals.average()
            val priorAvg  = if (priorVals.isEmpty()) 0.0 else priorVals.average()
            val change    = recentAvg - priorAvg
            val pctChange = if (priorAvg != 0.0) (change / priorAvg * 100) else 0.0

            val obj = JSONObject().apply {
                put("metric",          metric)
                put("period_days",     pDays)
                put("recent_avg",      recentAvg)
                put("prior_avg",       priorAvg)
                put("absolute_change", change)
                put("pct_change",      pctChange)
                put("direction",       if (change > 0) "up" else if (change < 0) "down" else "flat")
                put("recent_samples",  recentVals.size)
                put("prior_samples",   priorVals.size)
            }
            ToolResult.success(obj)
        }
    )

    private fun computeCorrelation() = CoachTool(
        name = "compute_correlation",
        specJson = SPEC_COMPUTE_CORRELATION,
        run = { args, ctx ->
            val o       = JSONObject(args)
            val metricA = o.optString("metric_a", "steps")
            val metricB = o.optString("metric_b", "hr")
            val days    = o.optInt("days", 14).coerceIn(7, 90)

            val seriesA = metricSeries(metricA, days, ctx)
            val seriesB = metricSeries(metricB, days, ctx)

            val aVals = seriesA.map { it.second }
            val bVals = seriesB.map { it.second }
            val corr  = pearsonCorr(aVals, bVals)

            val strength = when {
                abs(corr) >= 0.7 -> "strong"
                abs(corr) >= 0.4 -> "moderate"
                abs(corr) >= 0.2 -> "weak"
                else             -> "negligible"
            }

            val obj = JSONObject().apply {
                put("metric_a",        metricA)
                put("metric_b",        metricB)
                put("days",            days)
                put("correlation",     corr)
                put("strength",        strength)
                put("direction",       if (corr >= 0) "positive" else "negative")
                put("samples_a",       aVals.size)
                put("samples_b",       bVals.size)
            }
            ToolResult.success(obj)
        }
    )

    private fun detectOutliers() = CoachTool(
        name = "detect_outliers",
        specJson = SPEC_DETECT_OUTLIERS,
        run = { args, ctx ->
            val o      = JSONObject(args)
            val metric = o.optString("metric", "hr")
            val days   = o.optInt("days", 7).coerceIn(1, 90)
            val sigma  = o.optDouble("sigma", 2.0).coerceIn(1.5, 4.0)

            val series = metricSeries(metric, days, ctx)
            val values = series.map { it.second }
            if (values.isEmpty()) {
                return@CoachTool ToolResult.success(JSONObject().put("outliers", JSONArray()).put("count", 0))
            }

            val mean = values.average()
            val sd   = sqrt(values.map { (it - mean) * (it - mean) }.average())
            val threshold = mean + sigma * sd

            val arr = JSONArray()
            series.forEach { (ts, v) ->
                if (abs(v - mean) > sigma * sd) {
                    arr.put(JSONObject()
                        .put("at", isoString(ts))
                        .put("value", v)
                        .put("deviation", (v - mean) / sd)
                        .put("direction", if (v > mean) "high" else "low"))
                }
            }

            val obj = JSONObject().apply {
                put("metric",       metric)
                put("days",         days)
                put("sigma",        sigma)
                put("mean",         mean)
                put("std_dev",      sd)
                put("threshold",    threshold)
                put("outlier_count", arr.length())
                put("outliers",     arr)
            }
            ToolResult.success(obj)
        }
    )

    private fun summarizeDistribution() = CoachTool(
        name = "summarize_distribution",
        specJson = SPEC_SUMMARIZE_DISTRIBUTION,
        run = { args, ctx ->
            val o      = JSONObject(args)
            val metric = o.optString("metric", "hr")
            val days   = o.optInt("days", 7).coerceIn(1, 90)
            val values = metricValues(metric, days, ctx)

            if (values.isEmpty()) {
                return@CoachTool ToolResult.success(JSONObject().put("error", "No data for $metric in last $days days"))
            }

            val sorted = values.sorted()
            val p = { pct: Double -> sorted[(sorted.size * pct / 100).toInt().coerceIn(0, sorted.size - 1)] }
            val mean = values.average()
            val sd   = sqrt(values.map { (it - mean) * (it - mean) }.average())

            val obj = JSONObject().apply {
                put("metric",  metric)
                put("days",    days)
                put("count",   values.size)
                put("min",     sorted.first())
                put("p10",     p(10.0))
                put("p25",     p(25.0))
                put("median",  p(50.0))
                put("p75",     p(75.0))
                put("p90",     p(90.0))
                put("max",     sorted.last())
                put("mean",    mean)
                put("std_dev", sd)
            }
            ToolResult.success(obj)
        }
    )
}
