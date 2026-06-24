package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Circular activity-ring / progress ring showing a value relative to a goal.
 *
 * Matches the iOS ProgressRingView used in ActivityView. The ring starts at the 12 o'clock
 * position and sweeps clockwise. A darker track is drawn beneath the progress arc.
 *
 * The [content] slot renders in the centre of the ring (typically a numeric label).
 */
@Composable
fun ActivityRingView(
    value: Double,
    max: Double,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 12.dp,
    content: @Composable () -> Unit = {},
) {
    val colors = LocalPulseColors.current
    val trackColor = colors.elevated

    val fraction = if (max > 0.0) (value / max).toFloat().coerceIn(0f, 1f) else 0f
    val sweepAngle = 360f * fraction

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // Progress arc
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        content()
    }
}
