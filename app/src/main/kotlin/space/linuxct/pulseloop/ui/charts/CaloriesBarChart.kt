package space.linuxct.pulseloop.ui.charts

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
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import space.linuxct.pulseloop.domain.model.DailyMetricPoint
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

/**
 * Daily calories bar chart (7-day or 30-day).
 *
 * Mirrors the iOS CaloriesAreaChart visual language but adapts it to bar form to be consistent
 * with the other activity charts. Falls back to a "No data" placeholder on empty input.
 */
@Composable
fun CaloriesBarChart(
    points: List<DailyMetricPoint>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    height: Int = 150,
) {
    val colors = LocalPulseColors.current
    val unitKcal = stringResource(R.string.unit_kcal)

    if (points.isEmpty()) {
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

    val values = points.map { it.value }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries { series(y = values) }
        }
    }

    val caloriesColor = colors.calories

    val column = rememberLineComponent(
        fill = fill(caloriesColor.copy(alpha = 0.8f)),
        thickness = 16.dp,
        shape = CorneredShape.rounded(topLeftPercent = 50, topRightPercent = 50),
    )

    val columnProvider = ColumnCartesianLayer.ColumnProvider.series(column)

    val showLabels = labels.size == points.size && labels.any { it.isNotBlank() }

    val bottomAxis = if (showLabels) {
        val labelFormatter = CartesianValueFormatter { _, x, _ ->
            labels.getOrElse(x.toInt()) { "-" }
        }
        HorizontalAxis.rememberBottom(
            label = rememberAxisLabelComponent(color = colors.textMuted),
            valueFormatter = labelFormatter,
            guideline = null,
            line = null,
            tick = null,
        )
    } else null

    val marker = rememberPulseChartMarker(unitKcal)
    val layer = rememberColumnCartesianLayer(columnProvider = columnProvider)
    val chart = rememberCartesianChart(
        layer,
        bottomAxis = bottomAxis,
        marker = marker,
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxWidth().height(height.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
}
