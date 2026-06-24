package space.linuxct.pulseloop.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.linuxct.pulseloop.ui.screens.activity.ActivityDetailScreen
import space.linuxct.pulseloop.ui.screens.activity.ActivityScreen
import space.linuxct.pulseloop.ui.screens.coach.CoachScreen
import space.linuxct.pulseloop.ui.screens.debug.DebugScreen
import space.linuxct.pulseloop.ui.screens.onboarding.OnboardingFlowScreen
import space.linuxct.pulseloop.ui.screens.pairing.PairingScreen
import space.linuxct.pulseloop.ui.screens.record.RecordLiveScreen
import space.linuxct.pulseloop.ui.screens.record.RecordSelectScreen
import space.linuxct.pulseloop.ui.screens.record.RecordSummaryScreen
import space.linuxct.pulseloop.ui.screens.settings.SettingsScreen
import space.linuxct.pulseloop.ui.screens.shell.AppHeaderBar
import space.linuxct.pulseloop.ui.screens.sleep.SleepScreen
import space.linuxct.pulseloop.ui.screens.today.TodayScreen
import space.linuxct.pulseloop.ui.screens.vitals.VitalsScreen
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.ShellViewModel

val LocalBottomNavHeight = compositionLocalOf { 0.dp }

@Composable
fun AppShell(vm: ShellViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val onboardingCompleted by vm.onboardingCompleted.collectAsState()
    val pendingWorkoutId   by vm.pendingWorkoutDeepLink.collectAsState()
    var navigateToPairingAfterOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(pendingWorkoutId) {
        val id = pendingWorkoutId ?: return@LaunchedEffect
        vm.clearDeepLink()
        val route = if (vm.isFinished(id)) NavRoute.RecordSummary(id).route
                    else NavRoute.RecordLive(id).route
        navController.navigate(route)
    }

    LaunchedEffect(onboardingCompleted, navigateToPairingAfterOnboarding) {
        if (onboardingCompleted == true && navigateToPairingAfterOnboarding) {
            navigateToPairingAfterOnboarding = false
            navController.navigate(NavRoute.Pairing.route)
        }
    }

    val colors = LocalPulseColors.current
    when (onboardingCompleted) {
        null -> Box(
            modifier = Modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = colors.accent) }
        true -> MainTabShell(navController = navController)
        else -> OnboardingFlowScreen(
            onFinished = { vm.markOnboardingComplete() },
            onPairNow  = {
                navigateToPairingAfterOnboarding = true
                vm.markOnboardingComplete()
            }
        )
    }
}

@Composable
private fun MainTabShell(navController: NavHostController) {
    val colors   = LocalPulseColors.current
    val density  = LocalDensity.current
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    // Tabs that have their own top-level slot in the nav graph
    val tabRoutes = MainTab.entries.map { it.route }.toSet()
    val showBottomBar = currentRoute in tabRoutes

    val selectedTab = MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.TODAY

    var bottomNavHeightDp by remember { mutableStateOf(0.dp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        AppHeaderBar(
            onSettingsTap = { navController.navigate(NavRoute.Settings.route) },
            onDebugTap    = { navController.navigate(NavRoute.Debug.route) }
        )

        Box(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalBottomNavHeight provides bottomNavHeightDp) {
            NavHost(
                navController       = navController,
                startDestination    = NavRoute.Today.route,
                modifier            = Modifier.fillMaxSize(),
                enterTransition     = { fadeIn(tween(300)) },
                exitTransition      = { fadeOut(tween(300)) },
                popEnterTransition  = { fadeIn(tween(300)) },
                popExitTransition   = { fadeOut(tween(300)) },
            ) {
                // Tabs
                composable(NavRoute.Today.route)    { TodayScreen(navController) }
                composable(NavRoute.Vitals.route)   { VitalsScreen() }
                composable(NavRoute.Activity.route) { ActivityScreen(navController) }
                composable(NavRoute.Sleep.route)    { SleepScreen() }
                composable(NavRoute.Coach.route)    { CoachScreen(navController) }

                // Pushed
                composable(NavRoute.Settings.route) { SettingsScreen(navController) }
                composable(NavRoute.Pairing.route)  { PairingScreen(onDone = { navController.popBackStack() }) }
                composable(NavRoute.Debug.route)    { DebugScreen() }
                composable(NavRoute.RecordSelect.route) {
                    RecordSelectScreen(navController = navController)
                }
                composable(
                    route     = NavRoute.ActivityDetail.PATTERN,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) { back ->
                    val id = back.arguments?.getString("sessionId") ?: return@composable
                    ActivityDetailScreen(sessionId = id, navController = navController)
                }
                composable(
                    route     = NavRoute.RecordLive.PATTERN,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) { back ->
                    val id = back.arguments?.getString("sessionId") ?: return@composable
                    RecordLiveScreen(sessionId = id, navController = navController)
                }
                composable(
                    route     = NavRoute.RecordSummary.PATTERN,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) { back ->
                    val id = back.arguments?.getString("sessionId") ?: return@composable
                    RecordSummaryScreen(sessionId = id, navController = navController)
                }
            }

            } // CompositionLocalProvider

            if (showBottomBar) {
                BottomNavBar(
                    selected  = selectedTab,
                    onSelect  = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(NavRoute.Today.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { size ->
                            bottomNavHeightDp = with(density) { size.height.toDp() }
                        }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPulseColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.borderSubtle, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) }
                        )
                ) {
                    val iconBg = if (isSelected) colors.accentSoft else colors.background
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(iconBg)
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) colors.textPrimary else colors.textMuted
                        )
                    }
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) colors.textPrimary else colors.textMuted
                    )
                }
            }
        }
    }
}
