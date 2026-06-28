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
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Time-series heart-rate line chart.
 *
 * Renders BPM samples over a 24-hour or 7-day window. When fewer than 2 samples are supplied a
 * "No data" placeholder is shown instead of an empty chart.
 */
@Composable
fun HrLineChart(
    samples: List<MetricSample>,
    modifier: Modifier = Modifier,
    height: Int = 150,
) {
    val colors = LocalPulseColors.current
    val unitBpm = stringResource(R.string.unit_bpm)

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
    val lo = (values.min() - 5).coerceAtLeast(0.0)
    val hi = values.max() + 5

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples.size, samples.lastOrNull()?.timestamp) {
        modelProducer.runTransaction {
            lineSeries { series(y = values) }
        }
    }

    val lineColor = colors.heartRate

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

    val marker = rememberPulseChartMarker(unitBpm, samples, showGuideline = true)
    val chart = rememberCartesianChart(layer, marker = marker)

    val outerMod = if (modifier == Modifier) Modifier.fillMaxWidth().height(height.dp) else modifier
    Box(modifier = outerMod) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            drawTimeMarkers(samples)
        }
    }
}
