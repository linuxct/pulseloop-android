package space.linuxct.pulseloop.ui.screens.today.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.ui.res.stringResource
import space.linuxct.pulseloop.R
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.linuxct.pulseloop.domain.model.TimelineEvent
import space.linuxct.pulseloop.ui.components.BadgeVariant
import space.linuxct.pulseloop.ui.components.HealthMetricCard
import space.linuxct.pulseloop.ui.components.LargeScreenTitle
import space.linuxct.pulseloop.ui.components.StepRingGauge
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.today.averageLabel
import space.linuxct.pulseloop.ui.screens.today.formatSleepDuration
import space.linuxct.pulseloop.ui.screens.today.hrRangeLabel
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.TodayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TodayScreenV2(navController: NavController, vm: TodayViewModel) {
    val summary by vm.todaySummary.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        if (summary == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = scheme.primary)
            return@Box
        }

        val s = summary!!

        fun navigateTo(route: String) {
            navController.navigate(route) {
                popUpTo(NavRoute.Today.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                LargeScreenTitle(
                    title = stringResource(greetingForHour()),
                    subtitle = stringResource(R.string.today_screen_subtitle),
                    onSettingsTap = { navController.navigate(NavRoute.Settings.route) }
                )
            }

            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    StepRingGauge(steps = s.steps ?: 0, goal = s.goals.stepsDaily)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val hrValues   = s.trends.hrSamples24h.map { it.value }
                    val spo2Values = s.trends.spo2Samples24h.map { it.value }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                        HealthMetricCard(
                            title = stringResource(R.string.label_heart_rate),
                            value = hrRangeLabel(hrValues, s.latestHeartRate),
                            unit = if (hrValues.isNotEmpty() || s.latestHeartRate != null) stringResource(R.string.unit_bpm) else null,
                            color = scheme.error,
                            trend = hrValues,
                            badge = hrBadge(s.latestHeartRate)?.let { (id, variant) -> stringResource(id) to variant },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { navigateTo(NavRoute.Vitals.route) }
                        )
                        HealthMetricCard(
                            title = stringResource(R.string.label_spo2),
                            value = averageLabel(spo2Values, s.latestSpO2),
                            unit = if (spo2Values.isNotEmpty() || s.latestSpO2 != null) stringResource(R.string.unit_percent) else null,
                            color = scheme.tertiary,
                            trend = spo2Values,
                            badge = spo2Badge(s.latestSpO2)?.let { (id, variant) -> stringResource(id) to variant },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { navigateTo(NavRoute.Vitals.route) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                        val sleepMin = s.sleep?.totalMinutes ?: 0
                        HealthMetricCard(
                            title = stringResource(R.string.label_sleep),
                            value = s.sleep?.let { formatSleepDuration(it.totalMinutes) } ?: "—",
                            color = scheme.secondary,
                            badge = sleepBadge(sleepMin, s.goals.sleepHours)?.let { (id, variant) -> stringResource(id) to variant },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { navigateTo(NavRoute.Sleep.route) }
                        )
                        HealthMetricCard(
                            title = stringResource(R.string.label_calories),
                            value = s.calories?.let { "%,d".format(it.toInt()) } ?: "—",
                            unit = if (s.calories != null) stringResource(R.string.unit_kcal) else null,
                            color = scheme.primary,
                            trend = s.trends.calories7d.map { it.value },
                            // No ring calorie data → hide the sparkline rather than show a flat-zero graph.
                            showTrend = s.calories != null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { navigateTo(NavRoute.Activity.route) }
                        )
                    }
                    // Blood pressure + blood sugar (Jring only — shown once measured).
                    if (s.latestBloodPressureSystolic != null || s.latestBloodSugar != null) {
                        val pulseColors = LocalPulseColors.current
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                            HealthMetricCard(
                                title = stringResource(R.string.label_blood_pressure),
                                value = s.latestBloodPressureSystolic?.let { "${it.toInt()} / ${s.latestBloodPressureDiastolic?.toInt() ?: "--"}" } ?: "—",
                                unit = if (s.latestBloodPressureSystolic != null) stringResource(R.string.unit_mmhg) else null,
                                color = pulseColors.bloodPressure,
                                trend = s.trends.bpSysSamples24h.map { it.value },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { navigateTo(NavRoute.Vitals.route) }
                            )
                            HealthMetricCard(
                                title = stringResource(R.string.label_blood_sugar),
                                value = s.latestBloodSugar?.let { "%.0f".format(it) } ?: "—",
                                unit = if (s.latestBloodSugar != null) stringResource(R.string.unit_mgdl) else null,
                                color = pulseColors.bloodSugar,
                                trend = s.trends.glucoseSamples24h.map { it.value },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { navigateTo(NavRoute.Vitals.route) }
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.today_timeline_header), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    if (s.timeline.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            ) {
                                Text(stringResource(R.string.today_timeline_empty_title), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                                Text(stringResource(R.string.today_timeline_empty_message), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        s.timeline.take(5).forEach { event -> EventRowV2(event) }
                    }
                }
            }

            item { }
        }

    }
}

@Composable
private fun EventRowV2(event: TimelineEvent) {
    val scheme = MaterialTheme.colorScheme
    val dotColor = when (event.metric) {
        "hr"    -> scheme.error
        "spo2"  -> scheme.tertiary
        "sleep" -> scheme.secondary
        else    -> scheme.primary
    }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                Text(event.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(
                timeFmt.format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@StringRes
private fun greetingForHour(): Int = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..11  -> R.string.greeting_morning
    in 12..16 -> R.string.greeting_afternoon
    in 17..21 -> R.string.greeting_evening
    else      -> R.string.greeting_night
}

private fun hrBadge(latest: Double?): Pair<Int, BadgeVariant>? {
    val bpm = latest?.toInt() ?: return null
    return when {
        bpm in 60..100 -> R.string.badge_hr_in_range to BadgeVariant.GOOD
        bpm < 60       -> R.string.badge_hr_below_range to BadgeVariant.WARN
        else           -> R.string.badge_hr_above_range to BadgeVariant.WARN
    }
}

private fun spo2Badge(latest: Double?): Pair<Int, BadgeVariant>? {
    val pct = latest?.toInt() ?: return null
    return when {
        pct >= 95 -> R.string.badge_spo2_normal to BadgeVariant.GOOD
        pct >= 90 -> R.string.badge_spo2_low to BadgeVariant.WARN
        else      -> R.string.badge_spo2_very_low to BadgeVariant.WARN
    }
}

private fun sleepBadge(minutes: Int, goalHours: Double): Pair<Int, BadgeVariant>? {
    if (minutes <= 0) return null
    val goalMin = (goalHours * 60).toInt()
    return when {
        minutes >= goalMin          -> R.string.badge_goal_met to BadgeVariant.GOOD
        minutes >= (goalMin * 0.85) -> R.string.badge_almost_there to BadgeVariant.NEUTRAL
        else                        -> R.string.badge_short_night to BadgeVariant.WARN
    }
}
