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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Single-series day line chart with the full SpO2-style treatment: a touch tooltip with a dotted
 * vertical guideline (value + date) and dashed 00:00 / 06:00 / 12:00 / 18:00 time markers. Used for
 * blood sugar, fatigue and stress so the new diagrams match the existing HR/SpO2 charts. Auto-scales
 * the Y axis to the data; shows a "No data" placeholder under 2 samples.
 */
@Composable
fun MetricLineChart(
    samples: List<MetricSample>,
    color: Color,
    unit: String = "",
    modifier: Modifier = Modifier,
    height: Int = 120,
) {
    val colors = LocalPulseColors.current

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
    LaunchedEffect(samples.size, samples.lastOrNull()?.timestamp) {
        modelProducer.runTransaction { lineSeries { series(y = values) } }
    }

    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            remember(color) {
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(color)),
                    areaFill = null,
                )
            }
        ),
        rangeProvider = remember(lo, hi) { CartesianLayerRangeProvider.fixed(minY = lo, maxY = hi) },
    )

    val marker = rememberPulseChartMarker(unit, samples, showGuideline = true)
    val chart = rememberCartesianChart(layer, marker = marker)

    Box(modifier = modifier.fillMaxWidth().height(height.dp)) {
        // Time markers drawn underneath; chart (and its tooltip) renders on top.
        Canvas(modifier = Modifier.matchParentSize()) { drawTimeMarkers(samples) }
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
