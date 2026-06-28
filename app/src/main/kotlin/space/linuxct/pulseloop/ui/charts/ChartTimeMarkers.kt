package space.linuxct.pulseloop.ui.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.domain.model.MetricSample
import java.util.Calendar

// Draws dashed vertical lines at midnight (00:00) and midday (12:00) that fall within the
// visible sample window. Uses a neutral grey that is visually distinct from the touch-marker
// guideline (which is a solid, themed-color line at 20% alpha).
//
// X position is computed via fractional-index interpolation between the two samples that
// straddle each target timestamp — matching exactly how Vico distributes samples across
// the canvas width (by index, not by wall-clock time).
internal fun DrawScope.drawTimeMarkers(samples: List<MetricSample>) {
    if (samples.size < 2) return
    val minTs = samples.first().timestamp
    val maxTs = samples.last().timestamp
    if (maxTs <= minTs) return

    val n = samples.size
    val w = size.width
    val h = size.height

    val lineColor = Color(0xFF9E9E9E).copy(alpha = 0.28f)
    val labelColor = Color(0xFF9E9E9E).copy(alpha = 0.55f)

    val labelPaint = android.graphics.Paint().apply {
        textSize = 9.sp.toPx()
        color = labelColor.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    val dashPx = 4.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
    val strokePx = 1.dp.toPx()
    val labelBaselineY = 9.dp.toPx()
    val lineStartY = 13.dp.toPx()

    val cal = Calendar.getInstance()
    cal.timeInMillis = minTs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    repeat(3) {
        val midnight = cal.timeInMillis
        val sixAm   = midnight +  6L * 3_600_000L
        val noon    = midnight + 12L * 3_600_000L
        val sixPm   = midnight + 18L * 3_600_000L
        for ((ts, label) in listOf(midnight to "00:00", sixAm to "06:00", noon to "12:00", sixPm to "18:00")) {
            if (ts > minTs && ts < maxTs) {
                val fracIdx = fracIndexForTimestamp(ts, samples) ?: continue
                val x = w * fracIdx / (n - 1)
                drawLine(
                    color = lineColor,
                    start = Offset(x, lineStartY),
                    end = Offset(x, h),
                    strokeWidth = strokePx,
                    pathEffect = pathEffect
                )
                drawContext.canvas.nativeCanvas.drawText(label, x, labelBaselineY, labelPaint)
            }
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
}

// Returns a fractional sample index for the given target timestamp by linearly interpolating
// between the two adjacent samples that straddle it. Matches Vico's index-based x mapping.
private fun fracIndexForTimestamp(ts: Long, samples: List<MetricSample>): Float? {
    for (i in 0 until samples.size - 1) {
        val t0 = samples[i].timestamp
        val t1 = samples[i + 1].timestamp
        if (ts in t0..t1) {
            val frac = if (t1 != t0) (ts - t0).toFloat() / (t1 - t0).toFloat() else 0f
            return i + frac
        }
    }
    return null
}
