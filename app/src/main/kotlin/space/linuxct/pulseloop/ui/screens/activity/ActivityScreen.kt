package space.linuxct.pulseloop.ui.screens.activity

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.ui.screens.activity.v1.ActivityScreenV1
import space.linuxct.pulseloop.ui.screens.activity.v2.ActivityScreenV2
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.ActivityViewModel

@Composable
fun ActivityScreen(navController: NavController, vm: ActivityViewModel = hiltViewModel()) {
    if (LocalUiMode.current == UiMode.MATERIAL_YOU) {
        ActivityScreenV2(navController, vm)
    } else {
        ActivityScreenV1(navController, vm)
    }
}

internal fun activityLabel(type: String): String = when (type) {
    "run"   -> "Run"
    "walk"  -> "Walk"
    "cycle" -> "Cycle"
    "swim"  -> "Swim"
    "hike"  -> "Hike"
    else    -> type.replaceFirstChar { it.uppercase() }
}

internal fun formatDuration(seconds: Int): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
