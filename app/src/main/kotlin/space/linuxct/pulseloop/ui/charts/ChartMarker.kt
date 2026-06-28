package space.linuxct.pulseloop.ui.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberPulseChartMarker(
    unit: String = "",
    samples: List<MetricSample> = emptyList(),
    labelPosition: DefaultCartesianMarker.LabelPosition = DefaultCartesianMarker.LabelPosition.Top,
    showGuideline: Boolean = false,
): CartesianMarker {
    val colors = LocalPulseColors.current
    val bubbleBg = rememberShapeComponent(
        fill = fill(colors.tooltipBackground),
        shape = CorneredShape.Pill,
    )
    val label = rememberTextComponent(
        color = colors.tooltipText,
        textSize = 12.sp,
        lineCount = 2,
        textAlignment = android.text.Layout.Alignment.ALIGN_CENTER,
        padding = Dimensions(8f, 4f, 8f, 4f),
        background = bubbleBg,
    )
    val timeFmt = remember { SimpleDateFormat("dd-MM HH:mm", Locale.getDefault()) }
    val valueFormatter = remember(unit, samples.size, samples.lastOrNull()?.timestamp) {
        CartesianMarkerValueFormatter { _, targets ->
            val target = targets.firstOrNull() ?: return@CartesianMarkerValueFormatter ""
            val y = when (target) {
                is LineCartesianLayerMarkerTarget   -> target.points.firstOrNull()?.entry?.y
                is ColumnCartesianLayerMarkerTarget -> target.columns.firstOrNull()?.entry?.y
                else -> null
            } ?: return@CartesianMarkerValueFormatter ""
            val idx = target.x.toInt()
            val formatted = if (y >= 1_000) "%,.0f".format(y) else "%.0f".format(y)
            val valueLine = if (unit.isNotEmpty()) "$formatted $unit" else formatted
            val timestamp = samples.getOrNull(idx)?.timestamp
            val timeLine = timestamp?.let { "\n${timeFmt.format(Date(it))}" } ?: ""
            "$valueLine$timeLine"
        }
    }
    val guidelineColor = LocalPulseColors.current.textPrimary
    val guideline = if (showGuideline) {
        rememberAxisGuidelineComponent(fill = fill(guidelineColor.copy(alpha = 0.2f)))
    } else {
        null
    }
    return rememberDefaultCartesianMarker(label = label, valueFormatter = valueFormatter, labelPosition = labelPosition, guideline = guideline)
}
