package space.linuxct.pulseloop.ui.screens.sleep

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.ui.screens.sleep.v1.SleepScreenV1
import space.linuxct.pulseloop.ui.screens.sleep.v2.SleepScreenV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.SleepViewModel

@Composable
fun SleepScreen(vm: SleepViewModel = hiltViewModel()) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        SleepScreenV2(vm)
    } else {
        SleepScreenV1(vm)
    }
}

internal fun formatSleepDuration(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return if (h <= 0) "${m}m" else "${h}h ${"%02d".format(m)}m"
}
