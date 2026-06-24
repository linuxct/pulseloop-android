package space.linuxct.pulseloop.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.ui.charts.HrLineChart
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.RouteMapCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.ActivityDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    sessionId: String,
    navController: NavController,
    vm: ActivityDetailViewModel = hiltViewModel()
) {
    val colors = LocalPulseColors.current
    val state by vm.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.accent)
            return@Box
        }

        val session = state.session
        if (session == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Workout not found", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.textPrimary)
                Text("This session is no longer in local storage.", fontSize = 14.sp, color = colors.textMuted)
            }
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(activityLabel(session.activityType), color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Session header
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                activityLabel(session.activityType),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 24.sp,
                                color = colors.textPrimary
                            )
                            val dateFmt = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
                            Text(dateFmt.format(Date(session.startedAt)), fontSize = 13.sp, color = colors.textMuted)
                            val duration = session.finishedAt?.let { ((it - session.startedAt - session.elapsedPausedMs) / 1000).toInt() }
                            if (duration != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(formatDuration(duration), fontWeight = FontWeight.Medium, fontSize = 18.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }

                // Stats grid
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard("Distance", if (session.totalDistanceMeters > 0) "%.2f km".format(session.totalDistanceMeters / 1000) else "—", Modifier.weight(1f))
                        StatCard("Calories", if (session.totalCalories > 0) "%,d kcal".format(session.totalCalories.toInt()) else "—", Modifier.weight(1f))
                    }
                }

                // GPS route map (if points exist)
                if (state.gpsPoints.isNotEmpty()) {
                    item {
                        RouteMapCard(
                            points = state.gpsPoints,
                            interactive = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // HR chart (if available)
                if (state.hrSamples.size > 1) {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("HEART RATE", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                    HrLineChart(samples = state.hrSamples)
                                }
                            }
                        }
                    }
                }

                // No GPS placeholder (only when GPS was requested but nothing was recorded)
                if (session.useGps && state.gpsPoints.isEmpty()) {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            ) {
                                Text("No GPS route recorded", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                                Text("GPS points were not captured for this session.", fontSize = 12.sp, color = colors.textMuted)
                            }
                        }
                    }
                }

                // Delete button
                item {
                    SecondaryButton(
                        title = "Delete Workout",
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete this workout?") },
                text = { Text("This permanently removes the workout and its data. This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        vm.deleteSession { navController.popBackStack() }
                    }) { Text("Delete", color = colors.danger) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                },
                containerColor = colors.card
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.textPrimary)
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 0.6.sp)
        }
    }
}
