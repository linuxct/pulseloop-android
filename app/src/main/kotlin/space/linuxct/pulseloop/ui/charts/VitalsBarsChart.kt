package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import space.linuxct.pulseloop.domain.model.VitalsBar
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Vertical bar chart for historical HR / SpO2 daily averages.
 *
 * Present days use a solid [accentColor] bar; absent days use a faint placeholder.
 * Falls back to "No data" placeholder when [bars] is empty.
 */
@Composable
fun VitalsBarsChart(
    bars: List<VitalsBar>,
    accentColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    height: Int = 160,
    tooltipFormat: ((Double) -> String)? = null,
) {
    val colors = LocalPulseColors.current
    val chartBg = colors.cardSoft
    val borderColor = colors.borderSubtle
    val shape = RoundedCornerShape(16.dp)

    if (bars.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(shape)
                .background(chartBg)
                .border(1.dp, borderColor, shape),
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

    val presentValues = bars.mapNotNull { if (it.present) it.avgValue else null }
    val yMin = (presentValues.minOrNull() ?: 0.0).let { (it - it * 0.05).coerceAtLeast(0.0) }
    val yMax = (presentValues.maxOrNull() ?: 1.0).let { it + it * 0.05 }.coerceAtLeast(yMin + 1.0)
    val phantom = yMax

    // Absent days are drawn at yMax (phantom height) so a faint bar fills the slot.
    val values = bars.map { if (it.present && it.avgValue != null) it.avgValue else phantom }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(bars) {
        modelProducer.runTransaction { columnSeries { series(y = values) } }
    }

    val presentColumn = rememberLineComponent(
        fill = fill(accentColor),
        thickness = 16.dp,
        shape = CorneredShape.rounded(allPercent = 40),
    )
    val absentColumn = rememberLineComponent(
        fill = fill(accentColor.copy(alpha = 0.07f)),
        thickness = 16.dp,
        shape = CorneredShape.rounded(allPercent = 40),
    )

    val columnProvider = remember(bars, accentColor) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel.Entry,
                seriesIndex: Int,
                extraStore: com.patrykandpatrick.vico.core.common.data.ExtraStore,
            ) = if (bars.getOrNull(entry.x.toInt())?.present == true) presentColumn else absentColumn

            override fun getWidestSeriesColumn(
                seriesIndex: Int,
                extraStore: com.patrykandpatrick.vico.core.common.data.ExtraStore,
            ) = presentColumn
        }
    }

    val xLabelFmt = remember(bars) {
        CartesianValueFormatter { _, x, _ -> bars.getOrNull(x.toInt())?.label ?: " " }
    }

    val tooltipBg = rememberShapeComponent(fill = fill(colors.tooltipBackground), shape = CorneredShape.Pill)
    val tooltipLabel = rememberTextComponent(
        color = colors.tooltipText,
        textSize = 12.sp,
        padding = Dimensions(8f, 4f, 8f, 4f),
        background = tooltipBg,
    )
    val tooltipFmt = remember(bars, unit, tooltipFormat) {
        CartesianMarkerValueFormatter { _, targets ->
            val col = (targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget)
                ?.columns?.firstOrNull() ?: return@CartesianMarkerValueFormatter " "
            val bar = bars.getOrNull(col.entry.x.toInt()) ?: return@CartesianMarkerValueFormatter " "
            if (!bar.present) return@CartesianMarkerValueFormatter " "
            tooltipFormat?.invoke(bar.avgValue ?: 0.0) ?: "%.0f %s".format(bar.avgValue ?: 0.0, unit)
        }
    }
    val marker = rememberDefaultCartesianMarker(label = tooltipLabel, valueFormatter = tooltipFmt)

    val bottomAxis = HorizontalAxis.rememberBottom(
        label = rememberAxisLabelComponent(color = colors.textMuted),
        valueFormatter = xLabelFmt,
        guideline = null,
        line = null,
        tick = null,
    )

    val layer = rememberColumnCartesianLayer(
        columnProvider = columnProvider,
        rangeProvider = remember(yMin, yMax) { CartesianLayerRangeProvider.fixed(minY = yMin, maxY = yMax) },
    )

    val chart = rememberCartesianChart(layer, bottomAxis = bottomAxis, marker = marker)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(chartBg)
            .border(1.dp, borderColor, shape)
    ) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
