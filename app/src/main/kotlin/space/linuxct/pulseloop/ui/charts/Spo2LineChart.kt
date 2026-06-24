package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Time-series SpO2 scatter / dots chart.
 *
 * Mirrors the iOS SpO2DotsChart: a faint connecting line plus filled dot markers. The Y axis
 * is fixed 90–100 % to match clinical convention. Falls back to a "No data" placeholder when
 * fewer than 2 samples are available.
 */
@Composable
fun Spo2LineChart(
    samples: List<MetricSample>,
    modifier: Modifier = Modifier,
    height: Int = 150,
) {
    val colors = LocalPulseColors.current

    if (samples.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(height.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No data",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    val values = samples.map { it.value }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples.size, samples.lastOrNull()?.timestamp) {
        modelProducer.runTransaction {
            lineSeries { series(y = values) }
        }
    }

    val lineColor = colors.spo2

    // Draw the chart line via Vico (faint), then overlay dot markers using a Canvas.
    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            remember(lineColor) {
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(lineColor.copy(alpha = 0.25f))),
                    areaFill = null,
                )
            }
        ),
        rangeProvider = remember { CartesianLayerRangeProvider.fixed(minY = 90.0, maxY = 100.0) },
    )

    val marker = rememberPulseChartMarker("%", samples, showGuideline = true)
    val chart = rememberCartesianChart(layer, marker = marker)

    val outerMod = if (modifier == Modifier) Modifier.fillMaxWidth().height(height.dp) else modifier
    Box(modifier = outerMod) {
        // Dots drawn first so Vico's marker layer renders on top of them
        val dotColor = lineColor
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val minY = 90.0
            val maxY = 100.0
            val rangeY = maxY - minY

            values.forEachIndexed { i, v ->
                val x = if (values.size > 1) w * i / (values.size - 1) else w / 2f
                val yFrac = ((v - minY) / rangeY).toFloat().coerceIn(0f, 1f)
                val y = h - h * yFrac
                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }

        // Chart host on top so Vico's marker (tooltip) renders above the dots
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
