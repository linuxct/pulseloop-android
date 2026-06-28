package space.linuxct.pulseloop.ui.screens.activity.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivityRangeKey
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.domain.model.VitalsBar
import space.linuxct.pulseloop.ui.charts.StepsBarChart
import space.linuxct.pulseloop.ui.charts.VitalsBarsChart
import space.linuxct.pulseloop.ui.components.CalendarIconButton
import space.linuxct.pulseloop.ui.components.MetricTile
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.activity.activityLabel
import space.linuxct.pulseloop.ui.screens.activity.formatDuration
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ActivityScreenV1(navController: NavController, vm: ActivityViewModel) {
    val colors = LocalPulseColors.current
    val state by vm.uiState.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val finishedSessions = state.sessions.filter { it.statusRaw == ActivitySessionStatus.FINISHED.rawValue }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text(stringResource(R.string.screen_title_activity), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(stringResource(R.string.activity_screen_subtitle), fontSize = 14.sp, color = colors.textMuted)
                }
            }

            item {
                ActivityRangeSelectorV1(
                    selected = state.activityRange,
                    selectedDate = state.selectedDate,
                    onSelect = { vm.setActivityRange(it) },
                    onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setActivityRange(ActivityRangeKey.DAY) }
                )
            }

            if (state.activityRange == ActivityRangeKey.DAY) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricTile(title = stringResource(R.string.label_steps), value = state.todaySteps?.let { "%,d".format(it) } ?: "—", color = colors.steps, trend = state.steps7d.map { it.value }, modifier = Modifier.weight(1f))
                            MetricTile(title = stringResource(R.string.label_calories), value = state.todayCalories?.let { "%,d".format(it.toInt()) } ?: "—", unit = if (state.todayCalories != null) stringResource(R.string.unit_kcal) else null, color = colors.calories, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricTile(title = stringResource(R.string.label_distance), value = state.todayDistanceMeters?.let { "%.2f".format(it / 1000) } ?: "—", unit = if (state.todayDistanceMeters != null) stringResource(R.string.unit_km) else null, color = colors.distance, modifier = Modifier.weight(1f))
                            MetricTile(title = stringResource(R.string.label_active_minutes_short), value = state.todayActiveMinutes?.toString() ?: "—", unit = if (state.todayActiveMinutes != null) stringResource(R.string.unit_min) else null, color = colors.readiness, modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (state.steps7d.isNotEmpty()) {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.activity_steps_chart_title).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
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
                    ActivityHistoryCard(
                        title = stringResource(R.string.label_steps),
                        totalLabel = total,
                        bars = bars,
                        accentColor = colors.steps,
                        unit = "steps",
                        tooltipFormat = { "%.0f steps".format(it) },
                    )
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.calories, null, null, ab.present && (ab.calories ?: 0.0) > 0.0)
                    }
                    val unitKcal = stringResource(R.string.unit_kcal)
                    val total = state.caloriesRangeTotal?.let { "%,d $unitKcal".format(it.toInt()) }
                    ActivityHistoryCard(
                        title = stringResource(R.string.label_calories),
                        totalLabel = total,
                        bars = bars,
                        accentColor = colors.calories,
                        unit = unitKcal,
                        tooltipFormat = { "%.0f kcal".format(it) },
                    )
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.distanceMeters?.let { it / 1000.0 }, null, null, ab.present && (ab.distanceMeters ?: 0.0) > 0.0)
                    }
                    val unitKm = stringResource(R.string.unit_km)
                    val total = state.distanceRangeTotalM?.let { "%.2f $unitKm".format(it / 1000.0) }
                    ActivityHistoryCard(
                        title = stringResource(R.string.label_distance),
                        totalLabel = total,
                        bars = bars,
                        accentColor = colors.distance,
                        unit = unitKm,
                        tooltipFormat = { "%.2f km".format(it) },
                    )
                }

                item {
                    val bars = state.activityBars.map { ab ->
                        VitalsBar(ab.label, ab.activeMinutes?.toDouble(), null, null, ab.present && (ab.activeMinutes ?: 0) > 0)
                    }
                    val unitMin = stringResource(R.string.unit_min)
                    val total = state.activeMinutesRangeTotal?.let { "$it $unitMin" }
                    ActivityHistoryCard(
                        title = stringResource(R.string.label_active_minutes_short),
                        totalLabel = total,
                        bars = bars,
                        accentColor = colors.readiness,
                        unit = unitMin,
                        tooltipFormat = { "%.0f min".format(it) },
                    )
                }
            }

            if (state.activityRange == ActivityRangeKey.DAY) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.activity_recent_workouts_header).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.4.sp)
                        if (finishedSessions.isEmpty()) {
                            PulseCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                                    Text(stringResource(if (state.selectedDate != null) R.string.activity_no_workouts_title_past else R.string.activity_no_workouts_title), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                                    Text(stringResource(if (state.selectedDate != null) R.string.activity_no_workouts_message_past else R.string.activity_no_workouts_message), fontSize = 12.sp, color = colors.textMuted)
                                }
                            }
                        } else {
                            finishedSessions.take(10).forEach { session ->
                                WorkoutRow(session = session, onClick = { navController.navigate(NavRoute.ActivityDetail(session.id).route) })
                            }
                        }
                    }
                }
            }

            item { }
        }

        if (state.activityRange == ActivityRangeKey.DAY) {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoute.RecordSelect.route) },
                containerColor = colors.accent,
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 96.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_record_activity), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ActivityRangeSelectorV1(
    selected: ActivityRangeKey,
    selectedDate: Long?,
    onSelect: (ActivityRangeKey) -> Unit,
    onDateSelected: (Long?) -> Unit,
) {
    val colors = LocalPulseColors.current
    val options = listOf(
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
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(colors.cardSoft)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { (key, label) ->
                val isSelected = selected == key && selectedDate == null
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) colors.accent else colors.cardSoft)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(key); onDateSelected(null) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) Color.White else colors.textSecondary)
                }
            }
        }
        CalendarIconButton(selectedDateMs = selectedDate, onDateSelected = onDateSelected)
    }
}

@Composable
private fun ActivityHistoryCard(
    title: String,
    totalLabel: String?,
    bars: List<VitalsBar>,
    accentColor: Color,
    unit: String,
    tooltipFormat: ((Double) -> String)? = null,
) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
            if (totalLabel != null) {
                Text(totalLabel, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            VitalsBarsChart(bars = bars, accentColor = accentColor, unit = unit, modifier = Modifier.fillMaxWidth(), tooltipFormat = tooltipFormat)
        }
    }
}

@Composable
private fun WorkoutRow(session: ActivitySessionEntity, onClick: () -> Unit) {
    val colors = LocalPulseColors.current
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    PulseCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).background(colors.accentSoft, CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = colors.onAccentSoft, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(activityLabel(session.activityType), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                Text(dateFmt.format(Date(session.startedAt)), fontSize = 12.sp, color = colors.textMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (session.totalDistanceMeters > 0) Text("%.2f km".format(session.totalDistanceMeters / 1000), fontSize = 13.sp, color = colors.textSecondary)
                val duration = session.finishedAt?.let { ((it - session.startedAt - session.elapsedPausedMs) / 1000).toInt() }
                if (duration != null) Text(formatDuration(duration), fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}
