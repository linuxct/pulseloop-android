package space.linuxct.pulseloop.ui.screens.vitals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.ui.screens.vitals.v1.VitalsScreenV1
import space.linuxct.pulseloop.ui.screens.vitals.v2.VitalsScreenV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.VitalsViewModel

@Composable
fun VitalsScreen(vm: VitalsViewModel = hiltViewModel()) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        VitalsScreenV2(vm)
    } else {
        VitalsScreenV1(vm)
    }
}

@Composable
internal fun PulsingDot(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "spo2_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "spo2_alpha"
    )
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .size(10.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

internal fun hrRangeLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    val lo = values.min().toInt(); val hi = values.max().toInt()
    return if (lo == hi) "$lo" else "$lo-$hi"
}

internal fun averageLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    return (values.sum() / values.size).toInt().toString()
}
