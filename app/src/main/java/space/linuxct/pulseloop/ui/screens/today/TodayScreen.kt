package space.linuxct.pulseloop.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.domain.model.TimelineEvent
import space.linuxct.pulseloop.domain.model.TodaySummary
import space.linuxct.pulseloop.ui.components.MetricTile
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.TodayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(navController: NavController, vm: TodayViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val summary by vm.todaySummary.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (summary == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colors.accent
            )
            return@Box
        }

        val s = summary!!

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero insight card
            item { TodayHeroCard(s) }

            // 2x3 metric grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            title = "Steps",
                            value = s.steps?.let { "%,d".format(it) } ?: "—",
                            color = colors.steps,
                            trend = s.trends.steps7d.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Heart Rate",
                            value = hrRangeLabel(s.trends.hrSamples24h.map { it.value }, s.latestHeartRate),
                            unit = if (s.trends.hrSamples24h.isNotEmpty() || s.latestHeartRate != null) "bpm" else null,
                            color = colors.heartRate,
                            trend = s.trends.hrSamples24h.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            title = "SpO2",
                            value = averageLabel(s.trends.spo2Samples24h.map { it.value }, s.latestSpO2),
                            unit = if (s.trends.spo2Samples24h.isNotEmpty() || s.latestSpO2 != null) "%" else null,
                            color = colors.spo2,
                            trend = s.trends.spo2Samples24h.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Sleep",
                            value = s.sleep?.let { formatSleepDuration(((it.session.endAt - it.session.startAt) / 60_000).toInt()) } ?: "—",
                            color = colors.sleep,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricTile(
                            title = "Calories",
                            value = s.calories?.let { "%,d".format(it.toInt()) } ?: "—",
                            unit = if (s.calories != null) "kcal" else null,
                            color = colors.calories,
                            trend = s.trends.calories7d.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Distance",
                            value = s.distanceMeters?.let { "%.2f".format(it / 1000) } ?: "—",
                            unit = if (s.distanceMeters != null) "km" else null,
                            color = colors.distance,
                            trend = s.trends.distance7d.map { it.value },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Calibration banner
            if (s.calibration.isCalibrating) {
                item { CalibrationBanner(day = s.calibration.day, totalDays = s.calibration.totalDays) }
            }

            // Timeline section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TODAY SO FAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        letterSpacing = 1.4.sp
                    )
                    if (s.timeline.isEmpty()) {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Text("No events yet", fontWeight = FontWeight.Medium, color = colors.textPrimary, fontSize = 14.sp)
                                Text("Sync your ring to see activity here.", fontSize = 12.sp, color = colors.textMuted)
                            }
                        }
                    } else {
                        s.timeline.take(5).forEach { event ->
                            EventRow(event)
                        }
                    }
                }
            }

            item { /* bottom padding handled by contentPadding */ }
        }
    }
}

@Composable
private fun TodayHeroCard(summary: TodaySummary) {
    val colors = LocalPulseColors.current
    val hero = deriveHero(summary)

    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(hero.first, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.textPrimary)
            Text(hero.second, fontSize = 13.sp, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                hero.third.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.cardSoft)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(chip, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationBanner(day: Int, totalDays: Int) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp)
            ) {
                CircularProgressIndicator(
                    progress = { day.toFloat() / totalDays },
                    modifier = Modifier.size(36.dp),
                    color = colors.accent,
                    strokeWidth = 3.dp
                )
                Text("$day", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
            Column {
                Text("Learning your baseline", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                Text("Day $day of $totalDays — wear your ring through the day", fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun EventRow(event: TimelineEvent) {
    val colors = LocalPulseColors.current
    val dotColor = when (event.metric) {
        "hr" -> colors.heartRate
        "spo2" -> colors.spo2
        "sleep" -> colors.sleep
        else -> colors.accent
    }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    PulseCard(innerPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(dotColor, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                Text(event.detail, fontSize = 12.sp, color = colors.textMuted)
            }
            Text(
                timeFmt.format(Date(event.timestamp)),
                fontSize = 12.sp,
                color = colors.textMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun hrRangeLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    val lo = values.min().toInt()
    val hi = values.max().toInt()
    return if (lo == hi) "$lo" else "$lo-$hi"
}

private fun averageLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    return (values.sum() / values.size).toInt().toString()
}

private fun formatSleepDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h <= 0) "${m}m" else "${h}h ${"%02d".format(m)}m"
}

private fun deriveHero(today: TodaySummary): Triple<String, String, List<String>> {
    if (today.calibration.isCalibrating) {
        return Triple(
            "Learning your baseline",
            "Your ring is paired. Wear it through the day and sync once before bed so PulseLoop can start building your activity and recovery baseline.",
            listOf("Day ${today.calibration.day} of ${today.calibration.totalDays}", if (today.latestHeartRate == null) "HR pending" else "HR collected", if (today.sleep != null) "Sleep synced" else "Sleep pending")
        )
    }
    val steps = today.steps ?: return Triple(
        "Waiting for first sync",
        "Sync your ring to start collecting movement, heart rate, blood oxygen, and recovery context.",
        listOf("Baseline pending", if (today.latestHeartRate == null) "HR pending" else "HR collected", "Sleep pending")
    )
    val series = today.trends.steps7d.map { it.value }
    val stepsAvg = if (series.size > 1) series.dropLast(1).average() else series.average()
    val stepsDelta = if (stepsAvg > 0) (((steps.toDouble() - stepsAvg) / stepsAvg) * 100).toInt() else 0
    val title = when {
        stepsDelta >= 20 -> "Building momentum"
        stepsDelta <= -20 -> "Take it easy"
        else -> "Steady build"
    }
    val hrStr = today.latestHeartRate?.let { "${it.toInt()} bpm" } ?: "—"
    val summaryText = if (today.sleep == null)
        "You're at ${"%,d".format(steps)} steps. Sync after waking to add recovery context."
    else {
        val sleepMin = ((today.sleep.session.endAt - today.sleep.session.startAt) / 60_000).toInt()
        "You're at ${"%,d".format(steps)} steps, ${formatSleepDuration(sleepMin)} of sleep, and your latest reading is $hrStr."
    }
    val deltaLabel = if (series.size > 1) "Steps ${if (stepsDelta >= 0) "+" else ""}${stepsDelta}%" else "Steps collected"
    return Triple(title, summaryText, listOf(deltaLabel, if (today.latestHeartRate == null) "HR pending" else "HR collected", if (today.sleep != null) "Sleep synced" else "Sleep pending"))
}
