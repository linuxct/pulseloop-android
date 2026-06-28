package space.linuxct.pulseloop.ui.screens.activity.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivityRangeKey
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.domain.model.VitalsBar
import space.linuxct.pulseloop.ui.charts.StepsBarChart
import space.linuxct.pulseloop.ui.charts.VitalsBarsChart
import space.linuxct.pulseloop.ui.components.BadgeVariant
import space.linuxct.pulseloop.ui.components.CalendarIconButton
import space.linuxct.pulseloop.ui.components.HealthMetricCard
import space.linuxct.pulseloop.ui.components.LargeScreenTitle
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.activity.activityLabel
import space.linuxct.pulseloop.ui.screens.activity.formatDuration
import space.linuxct.pulseloop.ui.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ActivityScreenV2(navController: NavController, vm: ActivityViewModel) {
    val state by vm.uiState.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scheme = MaterialTheme.colorScheme
    val finishedSessions = state.sessions.filter { it.statusRaw == ActivitySessionStatus.FINISHED.rawValue }

    Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 140.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { LargeScreenTitle(title = stringResource(R.string.screen_title_activity), subtitle = stringResource(R.string.activity_screen_subtitle_v2)) }

            item {
                val rangeOptions = listOf(
                    ActivityRangeKey.DAY   to stringResource(R.string.activity_range_day),
                    ActivityRangeKey.WEEK  to stringResource(R.string.activity_range_week),
                    ActivityRangeKey.MONTH to stringResource(R.string.activity_range_month),
                    ActivityRangeKey.YEAR  to stringResource(R.string.activity_range_year),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        rangeOptions.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = state.activityRange == key && state.selectedDate == null,
                                onClick = { vm.setActivityRange(key); vm.setSelectedDate(null) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = rangeOptions.size),
                                label = { Text(label) }
                            )
                        }
                    }
                    CalendarIconButton(
                        selectedDateMs = state.selectedDate,
                        onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setActivityRange(ActivityRangeKey.DAY) }
                    )
                }
            }

            if (state.activityRange == ActivityRangeKey.DAY) {

                item {
                    val stepsBadge: Pair<String, BadgeVariant>? = state.todaySteps?.let { steps ->
                        val goal = state.goals.stepsDaily
                        when {
                            steps >= goal                 -> stringResource(R.string.badge_goal_met) to BadgeVariant.GOOD
                            steps >= (goal * 0.5).toInt() -> stringResource(R.string.badge_in_progress) to BadgeVariant.NEUTRAL
                            else                          -> stringResource(R.string.badge_keep_going) to BadgeVariant.WARN
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                            HealthMetricCard(title = stringResource(R.string.label_steps), value = state.todaySteps?.let { "%,d".format(it) } ?: "—", color = scheme.primary, trend = state.steps7d.map { it.value }, badge = stepsBadge, modifier = Modifier.weight(1f).fillMaxHeight())
                            HealthMetricCard(title = stringResource(R.string.label_calories), value = state.todayCalories?.let { "%,d".format(it.toInt()) } ?: "—", unit = if (state.todayCalories != null) stringResource(R.string.unit_kcal) else null, color = scheme.tertiary, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                            HealthMetricCard(title = stringResource(R.string.label_distance), value = state.todayDistanceMeters?.let { "%.2f".format(it / 1000) } ?: "—", unit = if (state.todayDistanceMeters != null) stringResource(R.string.unit_km) else null, color = scheme.secondary, modifier = Modifier.weight(1f).fillMaxHeight())
                            HealthMetricCard(title = stringResource(R.string.label_active_minutes_short), value = state.todayActiveMinutes?.toString() ?: "—", unit = if (state.todayActiveMinutes != null) stringResource(R.string.unit_min) else null, color = scheme.primary, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }

                if (state.steps7d.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(stringResource(R.string.activity_steps_chart_title), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                    StepsBarChart(points = state.steps7d, goal = state.goals.stepsDaily.toDouble())
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.steps?.toDouble(), null, null, ab.present && (ab.steps ?: 0) > 0)
                    }
                    val total = state.stepsRangeTotal?.let { "%,d steps".format(it) }
                    ActivityHistoryCardV2(title = stringResource(R.string.label_steps), totalLabel = total, bars = bars, accentColor = scheme.primary, unit = "steps", tooltipFormat = { "%.0f steps".format(it) })
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.calories, null, null, ab.present && (ab.calories ?: 0.0) > 0.0)
                    }
                    val unitKcal = stringResource(R.string.unit_kcal)
                    val total = state.caloriesRangeTotal?.let { "%,d $unitKcal".format(it.toInt()) }
                    ActivityHistoryCardV2(title = stringResource(R.string.label_calories), totalLabel = total, bars = bars, accentColor = scheme.tertiary, unit = unitKcal, tooltipFormat = { "%.0f kcal".format(it) })
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.distanceMeters?.let { it / 1000.0 }, null, null, ab.present && (ab.distanceMeters ?: 0.0) > 0.0)
                    }
                    val unitKm = stringResource(R.string.unit_km)
                    val total = state.distanceRangeTotalM?.let { "%.2f $unitKm".format(it / 1000.0) }
                    ActivityHistoryCardV2(title = stringResource(R.string.label_distance), totalLabel = total, bars = bars, accentColor = scheme.secondary, unit = unitKm, tooltipFormat = { "%.2f km".format(it) })
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.activeMinutes?.toDouble(), null, null, ab.present && (ab.activeMinutes ?: 0) > 0)
                    }
                    val unitMin = stringResource(R.string.unit_min)
                    val total = state.activeMinutesRangeTotal?.let { "$it $unitMin" }
                    ActivityHistoryCardV2(title = stringResource(R.string.label_active_minutes_short), totalLabel = total, bars = bars, accentColor = scheme.primary, unit = unitMin, tooltipFormat = { "%.0f min".format(it) })
                }
            }

            if (state.activityRange == ActivityRangeKey.DAY) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.activity_recent_workouts_header), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.padding(bottom = 4.dp))
                        if (finishedSessions.isEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                                    Text(stringResource(if (state.selectedDate != null) R.string.activity_no_workouts_title_past else R.string.activity_no_workouts_title), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                                    Text(stringResource(if (state.selectedDate != null) R.string.activity_no_workouts_message_past else R.string.activity_no_workouts_message_v2), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            finishedSessions.take(10).forEach { session ->
                                WorkoutRowV2(session = session, onClick = { navController.navigate(NavRoute.ActivityDetail(session.id).route) })
                            }
                        }
                    }
                }
            }

            item { }
        }

        if (state.activityRange == ActivityRangeKey.DAY) {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(NavRoute.RecordSelect.route) },
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 96.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_record)) }
            )
        }
    }
}

@Composable
private fun ActivityHistoryCardV2(
    title: String,
    totalLabel: String?,
    bars: List<VitalsBar>,
    accentColor: Color,
    unit: String,
    tooltipFormat: ((Double) -> String)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (totalLabel != null) {
                Text(totalLabel, style = MaterialTheme.typography.displaySmall, color = scheme.onSurface, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            VitalsBarsChart(bars = bars, accentColor = accentColor, unit = unit, modifier = Modifier.fillMaxWidth(), tooltipFormat = tooltipFormat)
        }
    }
}

@Composable
private fun WorkoutRowV2(session: ActivitySessionEntity, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val duration = session.finishedAt?.let { ((it - session.startedAt - session.elapsedPausedMs) / 1000).toInt() }
    ListItem(
        headlineContent = { Text(activityLabel(session.activityType), fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(dateFmt.format(Date(session.startedAt))) },
        leadingContent = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).background(scheme.primaryContainer, CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (session.totalDistanceMeters > 0) Text("%.2f km".format(session.totalDistanceMeters / 1000), style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
                if (duration != null) Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        colors = ListItemDefaults.colors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    )
}
