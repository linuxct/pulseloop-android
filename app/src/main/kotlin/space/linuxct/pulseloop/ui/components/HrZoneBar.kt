package space.linuxct.pulseloop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.theme.LocalPulseColors

// Vivid intensity palette for the five training zones: green → yellow → orange → red → purple.
private val ZoneColors = listOf(
    Color(0xFF22C55E), // Zone 1 — Warm Up
    Color(0xFFEAB308), // Zone 2 — Fat Burn
    Color(0xFFF97316), // Zone 3 — Aerobic
    Color(0xFFEF4444), // Zone 4 — Anaerobic
    Color(0xFF8B5CF6), // Zone 5 — Maximum
)

/**
 * Returns the heart-rate zone index (0..4) for [hr] given an estimated [maxHr],
 * or -1 when the reading sits below Zone 1 (< 50% of max HR).
 *
 * Zones follow the standard %-of-max model: Z1 50–60, Z2 60–70, Z3 70–80,
 * Z4 80–90, Z5 90–100+.
 */
fun hrZoneIndex(hr: Int, maxHr: Int): Int {
    if (maxHr <= 0) return -1
    val pct = hr.toFloat() / maxHr
    return when {
        pct < 0.50f -> -1
        pct < 0.60f -> 0
        pct < 0.70f -> 1
        pct < 0.80f -> 2
        pct < 0.90f -> 3
        else -> 4
    }
}

/**
 * Horizontal, colour-coded heart-rate-zone indicator for the live workout screen.
 *
 * The bar spans 50%–100% of [maxHr] in five equal segments and overlays a marker
 * tracking [currentHr]. When [currentHr] is null (no reading yet) the marker is
 * hidden and a "warming up" hint is shown instead.
 */
@Composable
fun HrZoneBar(
    currentHr: Int?,
    maxHr: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalPulseColors.current
    val zoneNames = listOf(
        stringResource(R.string.hr_zone_1_name),
        stringResource(R.string.hr_zone_2_name),
        stringResource(R.string.hr_zone_3_name),
        stringResource(R.string.hr_zone_4_name),
        stringResource(R.string.hr_zone_5_name),
    )
    val zoneIndex = currentHr?.let { hrZoneIndex(it, maxHr) } ?: -1

    // Marker position across the 50%..100% maxHR range (0f = Zone 1 start, 1f = max).
    val targetFraction = if (currentHr != null && maxHr > 0) {
        (((currentHr.toFloat() / maxHr) - 0.50f) / 0.50f).coerceIn(0f, 1f)
    } else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 600),
        label = "hrZoneMarker"
    )

    PulseCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_hr_zone).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    letterSpacing = 1.sp
                )
                if (currentHr != null && zoneIndex >= 0) {
                    Text(
                        stringResource(R.string.hr_zone_active_label, zoneIndex + 1, zoneNames[zoneIndex]),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZoneColors[zoneIndex]
                    )
                } else {
                    Text(
                        stringResource(R.string.hr_zone_warming_up),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val barHeight = 16.dp.toPx()
                val barTop = (size.height - barHeight) / 2f
                val barBottom = barTop + barHeight
                val radius = barHeight / 2f
                val segW = size.width / ZoneColors.size

                // Continuous bar: square inner divisions, rounded outer ends via clip.
                val barPath = Path().apply {
                    addRoundRect(RoundRect(0f, barTop, size.width, barBottom, radius, radius))
                }
                clipPath(barPath) {
                    ZoneColors.forEachIndexed { i, c ->
                        drawRect(
                            color = c,
                            topLeft = Offset(i * segW, barTop),
                            size = Size(segW, barHeight)
                        )
                    }
                }

                // Position marker only when there is a live reading.
                if (currentHr != null) {
                    val markerW = 5.dp.toPx()
                    val half = markerW / 2f
                    val x = (animatedFraction * size.width).coerceIn(half, size.width - half)
                    val outline = 1.5.dp.toPx()
                    val mr = markerW / 2f
                    // Dark outline for contrast against any segment colour.
                    drawRoundRect(
                        color = Color(0xCC0B0E14),
                        topLeft = Offset(x - half - outline, 0f),
                        size = Size(markerW + outline * 2, size.height),
                        cornerRadius = CornerRadius(mr + outline, mr + outline)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x - half, outline),
                        size = Size(markerW, size.height - outline * 2),
                        cornerRadius = CornerRadius(mr, mr)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Zone index labels beneath each segment.
            Row(modifier = Modifier.fillMaxWidth()) {
                ZoneColors.forEachIndexed { i, c ->
                    val active = i == zoneIndex
                    Text(
                        "${i + 1}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) c else c.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
