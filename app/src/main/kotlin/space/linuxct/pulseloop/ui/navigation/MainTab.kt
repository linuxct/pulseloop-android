package space.linuxct.pulseloop.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.ui.graphics.vector.ImageVector
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.navigation.NavRoute

enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String
) {
    TODAY   (R.string.tab_today,    Icons.Rounded.RadioButtonChecked,       NavRoute.Today.route),
    VITALS  (R.string.tab_vitals,   Icons.Rounded.FavoriteBorder,           NavRoute.Vitals.route),
    ACTIVITY(R.string.tab_activity, Icons.AutoMirrored.Rounded.ShowChart,   NavRoute.Activity.route),
    SLEEP   (R.string.tab_sleep,    Icons.Rounded.NightsStay,               NavRoute.Sleep.route),
    COACH   (R.string.tab_coach,    Icons.Rounded.Stars,                    NavRoute.Coach.route);
}
