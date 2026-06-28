package space.linuxct.pulseloop.ui.screens.today.v1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import space.linuxct.pulseloop.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import space.linuxct.pulseloop.domain.model.TimelineEvent
import space.linuxct.pulseloop.domain.model.TodaySummary
import space.linuxct.pulseloop.ui.components.MetricTile
import space.linuxct.pulseloop.ui.components.PulseCard
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
internal fun TodayScreenV1(navController: NavController, vm: TodayViewModel) {
    val colors = LocalPulseColors.current
    val summary by vm.todaySummary.collectAsState()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (summary == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.accent)
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
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { TodayHeroCard(s) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricTile(
                            title = stringResource(R.string.label_steps),
                            value = s.steps?.let { "%,d".format(it) } ?: "—",
                            color = colors.steps,
                            trend = s.trends.steps7d.map { it.value },
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Activity.route) }
                        )
                        val hrValues = s.trends.hrSamples24h.map { it.value }
                        MetricTile(
                            title = stringResource(R.string.label_heart_rate),
                            value = hrRangeLabel(hrValues, s.latestHeartRate),
                            unit = if (hrValues.isNotEmpty() || s.latestHeartRate != null) stringResource(R.string.unit_bpm) else null,
                            color = colors.heartRate,
                            trend = hrValues,
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Vitals.route) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        val spo2Values = s.trends.spo2Samples24h.map { it.value }
                        MetricTile(
                            title = stringResource(R.string.label_spo2),
                            value = averageLabel(spo2Values, s.latestSpO2),
                            unit = if (spo2Values.isNotEmpty() || s.latestSpO2 != null) stringResource(R.string.unit_percent) else null,
                            color = colors.spo2,
                            trend = spo2Values,
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Vitals.route) }
                        )
                        MetricTile(
                            title = stringResource(R.string.label_sleep),
                            value = s.sleep?.let { formatSleepDuration(((it.session.endAt - it.session.startAt) / 60_000).toInt()) } ?: "—",
                            color = colors.sleep,
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Sleep.route) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricTile(
                            title = stringResource(R.string.label_calories),
                            value = s.calories?.let { "%,d".format(it.toInt()) } ?: "—",
                            unit = if (s.calories != null) stringResource(R.string.unit_kcal) else null,
                            color = colors.calories,
                            trend = s.trends.calories7d.map { it.value },
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Activity.route) }
                        )
                        MetricTile(
                            title = stringResource(R.string.label_distance),
                            value = s.distanceMeters?.let { "%.2f".format(it / 1000) } ?: "—",
                            unit = if (s.distanceMeters != null) stringResource(R.string.unit_km) else null,
                            color = colors.distance,
                            trend = s.trends.distance7d.map { it.value },
                            modifier = Modifier.weight(1f),
                            onClick = { navigateTo(NavRoute.Activity.route) }
                        )
                    }
                    // Blood pressure + blood sugar (Jring only — shown once measured).
                    if (s.latestBloodPressureSystolic != null || s.latestBloodSugar != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricTile(
                                title = stringResource(R.string.label_blood_pressure),
                                value = s.latestBloodPressureSystolic?.let { "${it.toInt()} / ${s.latestBloodPressureDiastolic?.toInt() ?: "--"}" } ?: "—",
                                unit = if (s.latestBloodPressureSystolic != null) stringResource(R.string.unit_mmhg) else null,
                                color = colors.bloodPressure,
                                trend = s.trends.bpSysSamples24h.map { it.value },
                                modifier = Modifier.weight(1f),
                                onClick = { navigateTo(NavRoute.Vitals.route) }
                            )
                            MetricTile(
                                title = stringResource(R.string.label_blood_sugar),
                                value = s.latestBloodSugar?.let { "%.0f".format(it) } ?: "—",
                                unit = if (s.latestBloodSugar != null) stringResource(R.string.unit_mgdl) else null,
                                color = colors.bloodSugar,
                                trend = s.trends.glucoseSamples24h.map { it.value },
                                modifier = Modifier.weight(1f),
                                onClick = { navigateTo(NavRoute.Vitals.route) }
                            )
                        }
                    }
                }
            }

            if (s.calibration.isCalibrating) {
                item { CalibrationBanner(day = s.calibration.day, totalDays = s.calibration.totalDays) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.today_timeline_header).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.4.sp)
                    if (s.timeline.isEmpty()) {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                                Text(stringResource(R.string.today_timeline_empty_title), fontWeight = FontWeight.Medium, color = colors.textPrimary, fontSize = 14.sp)
                                Text(stringResource(R.string.today_timeline_empty_message), fontSize = 12.sp, color = colors.textMuted)
                            }
                        }
                    } else {
                        s.timeline.take(5).forEach { event -> EventRow(event) }
                    }
                }
            }

            item { }
        }
    }
}

@Composable
private fun TodayHeroCard(summary: TodaySummary) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    val hero = deriveHero(summary, context::getString)
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
                    ) { Text(chip, fontSize = 11.sp, color = colors.textSecondary) }
                }
            }
        }
    }
}

@Composable
private fun CalibrationBanner(day: Int, totalDays: Int) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                CircularProgressIndicator(
                    progress = { day.toFloat() / totalDays },
                    modifier = Modifier.size(36.dp),
                    color = colors.accent,
                    strokeWidth = 3.dp
                )
                Text("$day", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            }
            Column {
                Text(stringResource(R.string.calibration_banner_title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                Text(stringResource(R.string.calibration_banner_message, day, totalDays), fontSize = 12.sp, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun EventRow(event: TimelineEvent) {
    val colors = LocalPulseColors.current
    val dotColor = when (event.metric) {
        "hr" -> colors.heartRate; "spo2" -> colors.spo2; "sleep" -> colors.sleep; else -> colors.accent
    }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    PulseCard(innerPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(9.dp).background(dotColor, CircleShape))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
                Text(event.detail, fontSize = 12.sp, color = colors.textMuted)
            }
            Text(timeFmt.format(Date(event.timestamp)), fontSize = 12.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun deriveHero(today: TodaySummary, res: (Int) -> String): Triple<String, String, List<String>> {
    if (today.calibration.isCalibrating) {
        return Triple(
            res(R.string.calibration_banner_title),
            "Your ring is paired. Wear it through the day and sync once before bed so PulseLoop can start building your activity and recovery baseline.",
            listOf("Day ${today.calibration.day} of ${today.calibration.totalDays}", if (today.latestHeartRate == null) res(R.string.hero_chip_hr_pending) else res(R.string.hero_chip_hr_collected), if (today.sleep != null) res(R.string.hero_chip_sleep_synced) else res(R.string.hero_chip_sleep_pending))
        )
    }
    val steps = today.steps ?: return Triple(
        res(R.string.today_hero_waiting_title),
        res(R.string.today_hero_waiting_message),
        listOf(res(R.string.hero_chip_baseline_pending), if (today.latestHeartRate == null) res(R.string.hero_chip_hr_pending) else res(R.string.hero_chip_hr_collected), res(R.string.hero_chip_sleep_pending))
    )
    val series = today.trends.steps7d.map { it.value }
    val stepsAvg = if (series.size > 1) series.dropLast(1).average() else series.average()
    val stepsDelta = if (stepsAvg > 0) (((steps.toDouble() - stepsAvg) / stepsAvg) * 100).toInt() else 0
    val title = when {
        stepsDelta >= 20  -> res(R.string.today_hero_building_momentum)
        stepsDelta <= -20 -> res(R.string.today_hero_take_it_easy)
        else              -> res(R.string.today_hero_steady_build)
    }
    val hrStr = today.latestHeartRate?.let { "${it.toInt()} bpm" } ?: "—"
    val summaryText = if (today.sleep == null)
        "You're at ${"%,d".format(steps)} steps. Sync after waking to add recovery context."
    else {
        val sleepMin = ((today.sleep.session.endAt - today.sleep.session.startAt) / 60_000).toInt()
        "You're at ${"%,d".format(steps)} steps, ${formatSleepDuration(sleepMin)} of sleep, and your latest reading is $hrStr."
    }
    val deltaLabel = if (series.size > 1) "Steps ${if (stepsDelta >= 0) "+" else ""}${stepsDelta}%" else res(R.string.hero_chip_steps_collected)
    return Triple(title, summaryText, listOf(deltaLabel, if (today.latestHeartRate == null) res(R.string.hero_chip_hr_pending) else res(R.string.hero_chip_hr_collected), if (today.sleep != null) res(R.string.hero_chip_sleep_synced) else res(R.string.hero_chip_sleep_pending)))
}
