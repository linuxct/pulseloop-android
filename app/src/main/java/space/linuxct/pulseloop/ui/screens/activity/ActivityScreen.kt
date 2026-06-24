package space.linuxct.pulseloop.ui.screens.activity

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.ui.charts.StepsBarChart
import space.linuxct.pulseloop.ui.components.MetricTile
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(navController: NavController, vm: ActivityViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val state by vm.uiState.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val finishedSessions = state.sessions.filter { it.statusRaw == ActivitySessionStatus.FINISHED.rawValue }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text("Activity", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Record workouts and track movement from your ring", fontSize = 14.sp, color = colors.textMuted)
                }
            }

            // Today metric tiles
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            title = "Steps",
                            value = state.todaySteps?.let { "%,d".format(it) } ?: "—",
                            color = colors.steps,
                            trend = state.steps7d.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Calories",
                            value = state.todayCalories?.let { "%,d".format(it.toInt()) } ?: "—",
                            unit = if (state.todayCalories != null) "kcal" else null,
                            color = colors.calories,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            title = "Distance",
                            value = state.todayDistanceMeters?.let { "%.2f".format(it / 1000) } ?: "—",
                            unit = if (state.todayDistanceMeters != null) "km" else null,
                            color = colors.distance,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Active Min",
                            value = state.todayActiveMinutes?.toString() ?: "—",
                            unit = if (state.todayActiveMinutes != null) "min" else null,
                            color = colors.readiness,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7-day steps chart
            if (state.steps7d.isNotEmpty()) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "STEPS — LAST 7 DAYS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                StepsBarChart(
                                    points = state.steps7d,
                                    goal = state.goals.stepsDaily.toDouble()
                                )
                            }
                        }
                    }
                }
            }

            // Recent workouts
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "RECENT WORKOUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        letterSpacing = 1.4.sp
                    )
                    if (finishedSessions.isEmpty()) {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            ) {
                                Text("No workouts recorded yet", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                                Text("Start one manually when your ring misses an activity.", fontSize = 12.sp, color = colors.textMuted)
                            }
                        }
                    } else {
                        finishedSessions.take(10).forEach { session ->
                            WorkoutRow(session = session, onClick = {
                                navController.navigate(NavRoute.ActivityDetail(session.id).route)
                            })
                        }
                    }
                }
            }

            item { /* bottom padding handled by contentPadding */ }
        }

        FloatingActionButton(
            onClick = { navController.navigate(NavRoute.RecordSelect.route) },
            containerColor = colors.accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Record Activity", tint = Color.White)
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.accentSoft, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activityLabel(session.activityType),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                )
                Text(
                    dateFmt.format(Date(session.startedAt)),
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (session.totalDistanceMeters > 0) {
                    Text("%.2f km".format(session.totalDistanceMeters / 1000), fontSize = 13.sp, color = colors.textSecondary)
                }
                val duration = session.finishedAt?.let { ((it - session.startedAt - session.elapsedPausedMs) / 1000).toInt() }
                if (duration != null) {
                    Text(formatDuration(duration), fontSize = 12.sp, color = colors.textMuted)
                }
            }
        }
    }
}

internal fun activityLabel(type: String): String = when (type) {
    "run" -> "Run"
    "walk" -> "Walk"
    "cycle" -> "Cycle"
    "swim" -> "Swim"
    "hike" -> "Hike"
    else -> type.replaceFirstChar { it.uppercase() }
}

internal fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
