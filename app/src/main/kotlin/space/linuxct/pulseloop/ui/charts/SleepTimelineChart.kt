package space.linuxct.pulseloop.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.domain.model.SleepStage
import space.linuxct.pulseloop.domain.model.SleepSummary
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Sleep-stage palette matching the iOS SleepStageColors (Charts.swift)
private val SleepDeepColor  = Color(0xFF3F2DD8)
private val SleepLightColor = Color(0xFF7C5CFF)
private val SleepRemColor   = Color(0xFF2DD4D8)
private val SleepAwakeColor = Color(0xFFFFB86B)

private fun sleepStageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.DEEP    -> SleepDeepColor
    SleepStage.LIGHT   -> SleepLightColor
    SleepStage.REM     -> SleepRemColor
    SleepStage.AWAKE   -> SleepAwakeColor
    SleepStage.UNKNOWN -> Color(0xFF6F7A8C)
}

// Lane positions as fractions of canvas height (top → bottom: awake, rem, light, deep)
private fun stageLaneFrac(stage: SleepStage): Float = when (stage) {
    SleepStage.AWAKE   -> 0.15f
    SleepStage.REM     -> 0.38f
    SleepStage.LIGHT   -> 0.62f
    SleepStage.DEEP    -> 0.85f
    SleepStage.UNKNOWN -> 0.62f
}

private val sleepLaneOrder =
    listOf(SleepStage.AWAKE, SleepStage.REM, SleepStage.LIGHT, SleepStage.DEEP)

private data class BlockTap(val stage: SleepStage, val startMs: Long, val endMs: Long)

@Composable
fun SleepTimelineChart(
    sleep: SleepSummary,
    modifier: Modifier = Modifier,
    height: Int = 210,
) {
    val totalMinutes = maxOf(
        0,
        ((sleep.session.endAt - sleep.session.startAt) / 60_000L).toInt(),
    )
    SleepTimelineChart(
        blocks = sleep.blocks,
        totalMinutes = totalMinutes,
        startTimestamp = sleep.session.startAt,
        modifier = modifier,
        height = height,
    )
}

/**
 * Canvas-based horizontal hypnogram (sleep timeline) showing sleep stages as colored lanes.
 *
 * Touch or drag anywhere inside the chart to see a tooltip with the time range of the nearest
 * sleep stage block. The tooltip tracks the finger as it moves horizontally.
 */
@Composable
fun SleepTimelineChart(
    blocks: List<SleepStageBlockEntity>,
    totalMinutes: Int,
    startTimestamp: Long? = null,
    modifier: Modifier = Modifier,
    height: Int = 210,
) {
    val colors = LocalPulseColors.current
    val chartBg       = colors.cardSoft
    val borderColor   = colors.borderSubtle
    val connectorColor = colors.accent.copy(alpha = 0.35f)
    val shape = RoundedCornerShape(16.dp)

    if (blocks.isEmpty() || totalMinutes <= 0) {
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

    val sessionStart = startTimestamp ?: blocks.minOf { it.startAt }
    val sortedBlocks = blocks
        .filter { it.durationMinutes > 0 }
        .map { block ->
            val stage    = SleepStage.fromRaw(block.stageRaw)
            val startMin = ((block.startAt - sessionStart) / 60_000L).toInt().coerceAtLeast(0)
            Triple(stage, startMin, block.durationMinutes)
        }
        .filter { it.first != SleepStage.UNKNOWN }
        .sortedBy { it.second }

    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Time-tick labels at 0, ⅓, ⅔, end
    val ticks: List<Pair<Int, String>> = run {
        val offsets = listOf(0, totalMinutes / 3, totalMinutes * 2 / 3, totalMinutes)
        offsets.map { offset ->
            val label = if (startTimestamp != null) {
                timeFmt.format(Date(startTimestamp + offset * 60_000L))
            } else {
                "${offset / 60}h"
            }
            offset to label
        }
    }

    // Pre-resolve block timestamps for hit testing and tooltip display
    val resolvedBlocks = remember(sortedBlocks, sessionStart) {
        sortedBlocks.map { (stage, startMin, dur) ->
            BlockTap(
                stage   = stage,
                startMs = sessionStart + startMin * 60_000L,
                endMs   = sessionStart + (startMin + dur) * 60_000L,
            )
        }
    }

    var selectedBlock by remember { mutableStateOf<BlockTap?>(null) }
    var tapOffset     by remember { mutableStateOf(Offset.Zero) }
    var plotBoxSize   by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((height - 22).dp)
                .clip(shape)
                .background(chartBg)
                .border(1.dp, borderColor, shape)
                .onSizeChanged { plotBoxSize = it }
                .pointerInput(resolvedBlocks, totalMinutes) {
                    // Compute padding in px here — PointerInputScope extends Density
                    val leftPadPx  = 64.dp.toPx()
                    val rightPadPx = 16.dp.toPx()
                    val topPadPx   = 16.dp.toPx()
                    val botPadPx   = 16.dp.toPx()

                    awaitEachGesture {
                        // Fire immediately on finger-down, no tap threshold
                        val down = awaitFirstDown(requireUnconsumed = false)
                        updateSelection(down.position, resolvedBlocks, sessionStart,
                            totalMinutes, plotBoxSize,
                            leftPadPx, rightPadPx, topPadPx, botPadPx) { block, pos ->
                            selectedBlock = block; tapOffset = pos
                        }

                        // Track as finger drags — tooltip follows in real time
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            pressed = event.changes.any { it.pressed }
                            val pos = event.changes.firstOrNull()?.position
                            if (pos != null) {
                                event.changes.forEach { it.consume() }
                                if (pressed) {
                                    updateSelection(pos, resolvedBlocks, sessionStart,
                                        totalMinutes, plotBoxSize,
                                        leftPadPx, rightPadPx, topPadPx, botPadPx) { block, p ->
                                        selectedBlock = block; tapOffset = p
                                    }
                                }
                            }
                        }
                        // Clear when finger lifts
                        selectedBlock = null
                    }
                }
        ) {
            // Stage lane labels (left side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
            ) {
                sleepLaneOrder.forEachIndexed { i, stage ->
                    Text(
                        text = stage.rawValue.uppercase(),
                        color = sleepStageColor(stage),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = if (i < sleepLaneOrder.lastIndex)
                            Modifier.weight(1f) else Modifier,
                    )
                }
            }

            // Canvas for hypnogram lines + connectors
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 64.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                if (sortedBlocks.isEmpty()) return@Canvas

                val w = size.width
                val h = size.height

                fun xForMin(minute: Int) =
                    (minute.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f) * w

                fun yForStage(stage: SleepStage) = stageLaneFrac(stage) * h

                // Dashed vertical connectors
                for (i in 1 until sortedBlocks.size) {
                    val prev = sortedBlocks[i - 1]
                    val cur  = sortedBlocks[i]
                    val cx   = xForMin(cur.second)
                    drawLine(
                        color = connectorColor,
                        start = Offset(cx, yForStage(prev.first)),
                        end   = Offset(cx, yForStage(cur.first)),
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(2.5.dp.toPx(), 3.dp.toPx()),
                        ),
                    )
                }

                // Horizontal stage segments
                for ((stage, startMin, durationMin) in sortedBlocks) {
                    val y      = yForStage(stage)
                    val startX = xForMin(startMin)
                    val endX   = xForMin(startMin + durationMin).coerceAtLeast(startX)
                    val color  = sleepStageColor(stage)
                    val isSelected = selectedBlock?.let {
                        it.stage == stage && it.startMs == sessionStart + startMin * 60_000L
                    } == true

                    drawLine(
                        color = color.copy(alpha = if (isSelected) 0.30f else 0.16f),
                        start = Offset(startX, y),
                        end   = Offset(endX, y),
                        strokeWidth = if (isSelected) 16.dp.toPx() else 12.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = color,
                        start = Offset(startX, y),
                        end   = Offset(endX, y),
                        strokeWidth = if (isSelected) 8.dp.toPx() else 6.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Tooltip — floats above the finger; updates as the finger moves
            selectedBlock?.let { block ->
                val tooltipText = buildString {
                    append(block.stage.rawValue.uppercase())
                    append("  ")
                    append(timeFmt.format(Date(block.startMs)))
                    append(" – ")
                    append(timeFmt.format(Date(block.endMs)))
                }
                Box(
                    modifier = Modifier
                        .absoluteOffset {
                            val tooltipWPx = 178.dp.toPx()
                            val rawX = tapOffset.x - tooltipWPx / 2f
                            val clampedX = rawX.coerceIn(
                                8.dp.toPx(),
                                (plotBoxSize.width - tooltipWPx - 8.dp.toPx()).coerceAtLeast(8.dp.toPx()),
                            )
                            IntOffset(clampedX.roundToInt(), 8.dp.toPx().roundToInt())
                        }
                        .background(Color(0xEE0D1220), RoundedCornerShape(8.dp))
                        .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tooltipText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }

        // Time axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, end = 16.dp, top = 2.dp),
        ) {
            ticks.forEachIndexed { i, (_, label) ->
                Text(
                    text = label,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = if (i < ticks.lastIndex) Modifier.weight(1f) else Modifier,
                )
            }
        }
    }
}

/**
 * Finds the block nearest to [position] using x-axis proximity as the primary criterion
 * (any y position works) and y-distance as a tiebreaker when multiple blocks share the
 * same x range. Calls [onResult] with the winning block and the tap position.
 */
/**
 * Finds the block nearest to [position] (in Box coordinates) using x-axis proximity as the
 * primary criterion and y-distance as a tiebreaker. Works for any touch position — no y
 * threshold is applied, so touching anywhere in the chart always resolves to some block.
 */
private fun updateSelection(
    position: Offset,
    resolvedBlocks: List<BlockTap>,
    sessionStart: Long,
    totalMinutes: Int,
    boxSize: IntSize,
    leftPadPx: Float,
    rightPadPx: Float,
    topPadPx: Float,
    botPadPx: Float,
    onResult: (BlockTap, Offset) -> Unit,
) {
    if (resolvedBlocks.isEmpty() || boxSize.width == 0) return
    val canvasW = (boxSize.width  - leftPadPx - rightPadPx).coerceAtLeast(1f)
    val canvasH = (boxSize.height - topPadPx  - botPadPx).coerceAtLeast(1f)
    val relX = position.x - leftPadPx
    val relY = position.y - topPadPx

    val nearest = resolvedBlocks.minByOrNull { block ->
        val startX = ((block.startMs - sessionStart) / 60_000f / totalMinutes) * canvasW
        val endX   = ((block.endMs   - sessionStart) / 60_000f / totalMinutes) * canvasW
        val y      = stageLaneFrac(block.stage) * canvasH
        // x proximity is primary axis; y is only a tiebreaker (weight 0.05)
        val xDist = when {
            relX < startX -> startX - relX
            relX > endX   -> relX - endX
            else          -> 0f
        }
        xDist + abs(relY - y) * 0.05f
    } ?: return
    onResult(nearest, position)
}
