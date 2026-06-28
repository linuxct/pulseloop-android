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
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dual time-series line chart for blood pressure (systolic + diastolic plotted together), with the
 * same touch treatment as the other day charts: a tooltip showing both values as "123 / 88" with the
 * date on the line below, a dotted vertical guideline, and dashed 00:00/06:00/12:00/18:00 markers.
 * The two series are assumed aligned by the same measurement timestamps.
 */
@Composable
fun BloodPressureRangeChart(
    systolic: List<MetricSample>,
    diastolic: List<MetricSample>,
    systolicColor: Color,
    diastolicColor: Color,
    modifier: Modifier = Modifier,
    height: Int = 120,
) {
    val colors = LocalPulseColors.current

    if (systolic.size < 2) {
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

    val sysValues = systolic.map { it.value }
    val diaValues = diastolic.map { it.value }
    val all = sysValues + diaValues
    val lo = (all.min() - all.max() * 0.05).coerceAtLeast(0.0)
    val hi = all.max() + all.max() * 0.05

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(systolic, diastolic) {
        modelProducer.runTransaction {
            lineSeries {
                series(y = sysValues)
                if (diaValues.size >= 2) series(y = diaValues)
            }
        }
    }

    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            remember(systolicColor) {
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(systolicColor)),
                    areaFill = null,
                )
            },
            remember(diastolicColor) {
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(diastolicColor)),
                    areaFill = null,
                )
            },
        ),
        rangeProvider = remember(lo, hi) { CartesianLayerRangeProvider.fixed(minY = lo, maxY = hi) },
    )

    val marker = rememberBloodPressureMarker(systolic)
    val chart = rememberCartesianChart(layer, marker = marker)

    Box(modifier = modifier.fillMaxWidth().height(height.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) { drawTimeMarkers(systolic) }
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

/**
 * Marker that shows both BP series at the touched point as "systolic / diastolic" plus the date,
 * with a dotted vertical guideline — mirroring [rememberPulseChartMarker] but for two values.
 */
@Composable
private fun rememberBloodPressureMarker(samples: List<MetricSample>): CartesianMarker {
    val colors = LocalPulseColors.current
    val bubbleBg = rememberShapeComponent(fill = fill(colors.tooltipBackground), shape = CorneredShape.Pill)
    val label = rememberTextComponent(
        color = colors.tooltipText,
        textSize = 12.sp,
        lineCount = 2,
        textAlignment = android.text.Layout.Alignment.ALIGN_CENTER,
        padding = Dimensions(8f, 4f, 8f, 4f),
        background = bubbleBg,
    )
    val timeFmt = remember { SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()) }
    val valueFormatter = remember(samples.size, samples.lastOrNull()?.timestamp) {
        CartesianMarkerValueFormatter { _, targets ->
            val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
                ?: return@CartesianMarkerValueFormatter ""
            // Two series at this x; systolic is the larger. Sort descending so it reads "sys / dia".
            val ys = target.points.map { it.entry.y }.sortedDescending()
            val valueLine = when {
                ys.size >= 2 -> "${ys[0].toInt()} / ${ys[1].toInt()}"
                ys.size == 1 -> "${ys[0].toInt()}"
                else -> return@CartesianMarkerValueFormatter ""
            }
            val ts = samples.getOrNull(target.x.toInt())?.timestamp
            val timeLine = ts?.let { "\n${timeFmt.format(Date(it))}" } ?: ""
            "$valueLine$timeLine"
        }
    }
    val guideline = rememberAxisGuidelineComponent(fill = fill(colors.textPrimary.copy(alpha = 0.2f)))
    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = valueFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.Top,
        guideline = guideline,
    )
}
