package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import space.linuxct.pulseloop.domain.model.DailyMetricPoint
import space.linuxct.pulseloop.domain.model.MetricRange
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Simple time-series line chart for a list of [MetricSample]s.
 *
 * This is the overload called by VitalsScreen for stress, HRV, and temperature samples. It
 * renders a plain line chart using the provided [color] (defaults to `colors.accent`) and shows a
 * "No data" placeholder when fewer than 2 samples are available.
 */
@Composable
fun MetricRangeChart(
    samples: List<MetricSample>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Int = 150,
) {
    val colors = LocalPulseColors.current
    val lineColor = color ?: colors.accent

    if (samples.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(height.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.chart_empty_no_data),
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    val values = samples.map { it.value }
    val lo = (values.min() - values.max() * 0.05).coerceAtLeast(0.0)
    val hi = values.max() + values.max() * 0.05

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples) {
        modelProducer.runTransaction {
            lineSeries { series(y = values) }
        }
    }

    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            remember(lineColor) {
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                    areaFill = null,
                )
            }
        ),
        rangeProvider = remember(lo, hi) {
            CartesianLayerRangeProvider.fixed(minY = lo, maxY = hi)
        },
    )

    val marker = rememberPulseChartMarker(samples = samples)
    val chart = rememberCartesianChart(layer, marker = marker)

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(height.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}

/**
 * Range-selector container for chart composables.
 *
 * Renders range-selector tabs and delegates chart rendering to the [content] lambda.
 */
@Composable
fun MetricRangeSelectorChart(
    ranges: List<MetricRange> = MetricRange.entries,
    initialRange: MetricRange = MetricRange.TWENTY_FOUR_HOURS,
    modifier: Modifier = Modifier,
    onRangeChanged: ((MetricRange) -> Unit)? = null,
    content: @Composable (selectedRange: MetricRange) -> Unit,
) {
    val colors = LocalPulseColors.current
    var selected by remember { mutableStateOf(initialRange) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Range selector tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.elevated)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ranges.forEach { range ->
                val isActive = range == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) colors.card else Color.Transparent)
                        .clickable {
                            selected = range
                            onRangeChanged?.invoke(range)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = range.label(),
                        color = if (isActive) colors.textPrimary else colors.textMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        // Chart area
        content(selected)
    }
}

@Composable
private fun MetricRange.label(): String = when (this) {
    MetricRange.TWENTY_FOUR_HOURS -> stringResource(R.string.range_label_24h)
    MetricRange.SEVEN_DAYS -> stringResource(R.string.range_label_7d)
    MetricRange.THIRTY_DAYS -> stringResource(R.string.range_label_30d)
    MetricRange.TWELVE_MONTHS -> stringResource(R.string.range_label_12mo)
}

/**
 * Range-selector wrapper for time-series line charts.
 */
@Composable
fun MetricRangeLineChart(
    samplesForRange: (MetricRange) -> List<MetricSample>,
    lineChart: @Composable (samples: List<MetricSample>, modifier: Modifier) -> Unit,
    ranges: List<MetricRange> = MetricRange.entries,
    initialRange: MetricRange = MetricRange.TWENTY_FOUR_HOURS,
    modifier: Modifier = Modifier,
    chartHeight: Int = 150,
) {
    MetricRangeSelectorChart(
        ranges = ranges,
        initialRange = initialRange,
        modifier = modifier,
    ) { selectedRange ->
        val samples = remember(selectedRange) { samplesForRange(selectedRange) }
        lineChart(samples, Modifier.fillMaxWidth().height(chartHeight.dp).padding(top = 12.dp))
    }
}

/**
 * Range-selector wrapper for daily bar charts.
 */
@Composable
fun MetricRangeBarChart(
    pointsForRange: (MetricRange) -> List<DailyMetricPoint>,
    barChart: @Composable (points: List<DailyMetricPoint>, modifier: Modifier) -> Unit,
    ranges: List<MetricRange> = listOf(
        MetricRange.SEVEN_DAYS,
        MetricRange.THIRTY_DAYS,
        MetricRange.TWELVE_MONTHS,
    ),
    initialRange: MetricRange = MetricRange.SEVEN_DAYS,
    modifier: Modifier = Modifier,
    chartHeight: Int = 160,
) {
    MetricRangeSelectorChart(
        ranges = ranges,
        initialRange = initialRange,
        modifier = modifier,
    ) { selectedRange ->
        val points = remember(selectedRange) { pointsForRange(selectedRange) }
        barChart(points, Modifier.fillMaxWidth().height(chartHeight.dp).padding(top = 12.dp))
    }
}
