package space.linuxct.pulseloop.ui.screens.today

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.ui.screens.today.v1.TodayScreenV1
import space.linuxct.pulseloop.ui.screens.today.v2.TodayScreenV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.TodayViewModel

@Composable
fun TodayScreen(navController: NavController, vm: TodayViewModel = hiltViewModel()) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        TodayScreenV2(navController, vm)
    } else {
        TodayScreenV1(navController, vm)
    }
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

internal fun formatSleepDuration(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return if (h <= 0) "${m}m" else "${h}h ${"%02d".format(m)}m"
}
