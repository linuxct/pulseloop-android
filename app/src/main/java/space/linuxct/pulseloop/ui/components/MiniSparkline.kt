package space.linuxct.pulseloop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun MiniSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(1.0)
        val w = size.width
        val h = size.height

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = w * i / (values.size - 1)
            val y = h - h * ((v - min) / range).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = if (values.size > 1) 0.9f else 0.2f),
            style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
