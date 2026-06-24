package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import space.linuxct.pulseloop.coach.model.CoachChart
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

@Composable
fun CoachChartCard(chart: CoachChart, modifier: Modifier = Modifier) {
    val colors = LocalPulseColors.current
    val values = chart.data.map { it.value }
    val labels = chart.data.map { it.label }

    if (values.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(chart.data) {
        modelProducer.runTransaction {
            if (chart.type == "line") {
                lineSeries { series(y = values) }
            } else {
                columnSeries { series(y = values) }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (chart.title.isNotBlank()) {
            Text(
                chart.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val xFormatter = CartesianValueFormatter { _, x, _ ->
            labels.getOrElse(x.toInt()) { "" }
        }
        val yFormatter = CartesianValueFormatter { _, y, _ ->
            formatYLabel(y, chart.unit)
        }

        val bottomAxis = HorizontalAxis.rememberBottom(
            label = rememberAxisLabelComponent(color = colors.textMuted, textSize = 10.sp),
            valueFormatter = xFormatter,
            guideline = null,
            line = null,
            tick = null,
        )
        val startAxis = VerticalAxis.rememberStart(
            label = rememberAxisLabelComponent(color = colors.textMuted, textSize = 10.sp),
            valueFormatter = yFormatter,
            guideline = null,
            line = null,
            tick = null,
        )

        if (chart.type == "line") {
            val lo = (values.min() * 0.9).coerceAtLeast(0.0)
            val hi = values.max() * 1.1
            val accentColor = colors.accent
            val layer = rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    remember(accentColor) {
                        LineCartesianLayer.Line(
                            fill = LineCartesianLayer.LineFill.single(fill(accentColor)),
                            areaFill = null,
                        )
                    }
                ),
                rangeProvider = remember(lo, hi) {
                    CartesianLayerRangeProvider.fixed(minY = lo, maxY = hi)
                },
            )
            CartesianChartHost(
                chart = rememberCartesianChart(layer, startAxis = startAxis, bottomAxis = bottomAxis),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
            )
        } else {
            val accentColor = colors.accent
            val column = rememberLineComponent(
                fill = fill(accentColor.copy(alpha = 0.75f)),
                thickness = 16.dp,
                shape = CorneredShape.rounded(topLeftPercent = 50, topRightPercent = 50),
            )
            val layer = rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(column)
            )
            CartesianChartHost(
                chart = rememberCartesianChart(layer, startAxis = startAxis, bottomAxis = bottomAxis),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
            )
        }
    }
}

/** Formats a y-axis value with abbreviated magnitude and the chart unit. */
private fun formatYLabel(y: Double, unit: String): String {
    val num = when {
        y >= 10_000 -> "${(y / 1_000).toInt()}k"
        y >= 1_000  -> "${"%.1f".format(y / 1_000)}k"
        y == y.toLong().toDouble() -> y.toLong().toString()
        else -> "%.1f".format(y)
    }
    return if (unit.isNotBlank()) "$num $unit" else num
}
