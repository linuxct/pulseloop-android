package space.linuxct.pulseloop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.ui.graphics.vector.ImageVector
import space.linuxct.pulseloop.ui.navigation.NavRoute

enum class MainTab(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    TODAY   ("Today",    Icons.Rounded.RadioButtonChecked, NavRoute.Today.route),
    VITALS  ("Vitals",   Icons.Rounded.FavoriteBorder,    NavRoute.Vitals.route),
    ACTIVITY("Activity", Icons.AutoMirrored.Rounded.ShowChart,          NavRoute.Activity.route),
    SLEEP   ("Sleep",    Icons.Rounded.NightsStay,         NavRoute.Sleep.route),
    COACH   ("Coach",    Icons.Rounded.Stars,              NavRoute.Coach.route);
}
