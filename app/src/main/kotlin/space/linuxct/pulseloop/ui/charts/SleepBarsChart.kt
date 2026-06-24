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
import androidx.compose.ui.graphics.toArgb
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
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import space.linuxct.pulseloop.domain.model.SleepBar
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

// Sleep-bar gradient colors matching iOS SleepDurationHistogramChart
private val SleepBarTop = Color(0xFF8B7CFF)
private val SleepBarBottom = Color(0xFF3F2DD8)

/**
 * Vertical bar chart for nightly sleep duration.
 *
 * Mirrors the iOS SleepDurationHistogramChart. Present nights use a purple gradient bar,
 * absent slots use a faint placeholder bar. An optional goal line is drawn as a dashed rule.
 * Falls back to "No sleep data" placeholder when [bars] is empty.
 */
@Composable
fun SleepBarsChart(
    bars: List<SleepBar>,
    goalMin: Int? = null,
    goalMinutes: Int? = null,
    slim: Boolean = false,
    modifier: Modifier = Modifier,
    height: Int = 210,
) {
    // Accept either goalMin or goalMinutes as parameter name (for call-site compatibility)
    @Suppress("NAME_SHADOWING")
    val goalMin = goalMinutes ?: goalMin
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
                text = "No sleep data",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    // Compute yMax
    val maxDuration = bars.mapNotNull { it.durationMin }.maxOrNull() ?: 0
    val ceiling = maxOf(maxDuration, goalMin ?: 0)
    val yMax = if (ceiling > 0) ceiling.toDouble() * 1.15 else 600.0

    // For present bars use actual duration; for absent bars use yMax (faint placeholder)
    val values = bars.map { bar ->
        if (bar.present && bar.durationMin != null) bar.durationMin.toDouble() else yMax
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(bars) {
        modelProducer.runTransaction {
            columnSeries { series(y = values) }
        }
    }

    // Present bars: sleep purple; absent bars: very faint accent
    val accentFaint = colors.accent.copy(alpha = 0.05f)
    val barWidth = if (slim) 7.dp else 14.dp

    // We can't do per-bar fill easily in Vico 2.x, so we draw both columns via the
    // same single-series approach but build two separate columnProviders and pick based
    // on actual values by using a custom ColumnProvider.
    val presentColumn = rememberLineComponent(
        fill = fill(SleepBarTop),
        thickness = barWidth,
        shape = CorneredShape.rounded(
            topLeftPercent = if (slim) 30 else 50,
            topRightPercent = if (slim) 30 else 50,
        ),
    )
    val absentColumn = rememberLineComponent(
        fill = fill(accentFaint),
        thickness = barWidth,
        shape = CorneredShape.rounded(
            topLeftPercent = if (slim) 30 else 50,
            topRightPercent = if (slim) 30 else 50,
        ),
    )

    // A custom ColumnProvider that selects the right LineComponent based on whether the bar
    // is present (below yMax) or absent (equals yMax).
    val columnProvider = remember(bars, yMax, slim) {
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

    val labelFormatter = CartesianValueFormatter { _, x, _ ->
        bars.getOrNull(x.toInt())?.label?.takeIf { it.isNotBlank() } ?: "-"
    }

    val bottomAxis = HorizontalAxis.rememberBottom(
        label = rememberAxisLabelComponent(color = colors.textMuted),
        valueFormatter = labelFormatter,
        guideline = null,
        line = null,
        tick = null,
    )

    val goalLine = if (goalMin != null && goalMin > 0) {
        rememberLineComponent(
            fill = fill(colors.textMuted.copy(alpha = 0.5f)),
            thickness = 1.dp,
            shape = DashedShape(
                shape = com.patrykandpatrick.vico.core.common.shape.Shape.Rectangle,
                dashLengthDp = 4f,
                gapLengthDp = 4f,
            ),
        )
    } else null

    val decorations = if (goalLine != null && goalMin != null) {
        listOf(HorizontalLine(y = { goalMin.toDouble() }, line = goalLine))
    } else emptyList()

    val sleepMarkerBg = rememberShapeComponent(fill = fill(Color(0xFF1E2D45)), shape = CorneredShape.Pill)
    val sleepMarkerLabel = rememberTextComponent(
        color = Color.White,
        textSize = 12.sp,
        padding = Dimensions(8f, 4f, 8f, 4f),
        background = sleepMarkerBg,
    )
    val sleepMarkerFormatter = remember(bars) {
        CartesianMarkerValueFormatter { _, targets ->
            val col = (targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget)
                ?.columns?.firstOrNull() ?: return@CartesianMarkerValueFormatter ""
            val bar = bars.getOrNull(col.entry.x.toInt()) ?: return@CartesianMarkerValueFormatter ""
            if (!bar.present || bar.durationMin == null) return@CartesianMarkerValueFormatter ""
            val h = bar.durationMin / 60
            val m = bar.durationMin % 60
            when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }
    }
    val marker = rememberDefaultCartesianMarker(label = sleepMarkerLabel, valueFormatter = sleepMarkerFormatter)

    val layer = rememberColumnCartesianLayer(
        columnProvider = columnProvider,
        rangeProvider = remember(yMax) {
            CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = yMax)
        },
    )
    val chart = rememberCartesianChart(
        layer,
        bottomAxis = bottomAxis,
        decorations = decorations,
        marker = marker,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(chartBg)
            .border(1.dp, borderColor, shape)
            .fillMaxWidth()
    ) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
