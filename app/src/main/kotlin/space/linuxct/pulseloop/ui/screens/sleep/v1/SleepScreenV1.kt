package space.linuxct.pulseloop.ui.screens.sleep.v1

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.SleepRangeKey
import space.linuxct.pulseloop.domain.model.SleepSummary
import space.linuxct.pulseloop.domain.service.SleepInsights
import space.linuxct.pulseloop.domain.service.SleepScore
import space.linuxct.pulseloop.ui.charts.SleepBarsChart
import space.linuxct.pulseloop.ui.charts.SleepTimelineChart
import space.linuxct.pulseloop.ui.components.CalendarIconButton
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.screens.sleep.formatSleepDuration
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.SleepViewModel

@Composable
internal fun SleepScreenV1(vm: SleepViewModel) {
    val colors = LocalPulseColors.current
    val state by vm.uiState.collectAsState()
    val range = state.range
    val rangeSummary = state.rangeSummary
    val validSessions = rangeSummary?.let { SleepInsights.validSessions(it.sessions) } ?: emptyList()
    val lastNight = if (range == SleepRangeKey.DAY) validSessions.lastOrNull() else null
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text(stringResource(R.string.screen_title_sleep), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(stringResource(R.string.sleep_screen_subtitle), fontSize = 14.sp, color = colors.textMuted)
                }
            }

            item {
                RangeSelector(
                    selected = range,
                    selectedDate = state.selectedDate,
                    onSelect = { vm.setRange(it) },
                    onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setRange(SleepRangeKey.DAY) }
                )
            }

            if (range == SleepRangeKey.DAY) {
                if (lastNight != null) {
                    item { SleepHeroCard(sleep = lastNight) }

                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text(stringResource(R.string.sleep_stages_header).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.8.sp)
                                        Text(stringResource(R.string.sleep_stages_subtitle), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LegendDot(stringResource(R.string.sleep_stage_deep), colors.sleep)
                                        LegendDot(stringResource(R.string.sleep_stage_light), colors.spo2)
                                        LegendDot(stringResource(R.string.sleep_stage_awake), colors.warning)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (lastNight.blocks.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) { SleepTimelineChart(sleep = lastNight) }
                                } else {
                                    Text(stringResource(R.string.sleep_no_stage_data), fontSize = 13.sp, color = colors.textMuted)
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            StageTile(stringResource(R.string.sleep_stage_deep), formatSleepDuration(lastNight.deepMinutes), Modifier.weight(1f))
                            StageTile(stringResource(R.string.sleep_stage_light), formatSleepDuration(lastNight.lightMinutes), Modifier.weight(1f))
                            StageTile(stringResource(R.string.sleep_stage_awake), formatSleepDuration(lastNight.awakeMinutes), Modifier.weight(1f))
                        }
                    }

                    item {
                        val score = SleepScore.calculate(lastNight)
                        val coach = SleepInsights.dayCoach(lastNight, score.score, score.awakePct, score.deepPct, null)
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(coach.headline, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                                Text(coach.body, fontSize = 13.sp, color = colors.textSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    coach.chips.forEach { chip ->
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
                } else {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                                Text(stringResource(if (state.selectedDate != null) R.string.sleep_no_sleep_title_past else R.string.sleep_no_sleep_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
                                Text(stringResource(if (state.selectedDate != null) R.string.sleep_no_sleep_message_past else R.string.sleep_no_sleep_message), fontSize = 13.sp, color = colors.textMuted)
                            }
                        }
                    }
                }
            } else {
                val avgMin = SleepInsights.averageDuration(validSessions)
                val avgScore = SleepInsights.averageScore(validSessions)
                val enough = validSessions.size >= 2

                item {
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(SleepInsights.rangeHeroLabel[range] ?: "Sleep", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textMuted)
                            Text(if (enough && avgMin != null) "${formatSleepDuration(avgMin)} avg" else "Not enough data", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (avgScore != null && enough) Text("Score: $avgScore · ${SleepScore.qualityLabel(avgScore).label}", fontSize = 13.sp, color = colors.textSecondary)
                            Text("${validSessions.size} of ${rangeSummary?.expectedNights ?: "?"} nights tracked", fontSize = 12.sp, color = colors.textMuted)
                        }
                    }
                }

                if (state.bars.isNotEmpty()) {
                    item {
                        PulseCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.label_duration).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) { SleepBarsChart(bars = state.bars, goalMinutes = state.goalMinutes) }
                            }
                        }
                    }
                }

                item {
                    val coach = SleepInsights.aggregateCoach(range, rangeSummary?.sessions ?: emptyList(), rangeSummary?.expectedNights ?: 0, state.goalMinutes)
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(coach.headline, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                            Text(coach.body, fontSize = 13.sp, color = colors.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                coach.chips.forEach { chip ->
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
            }

            item { }
        }
    }
}

@Composable
private fun SleepHeroCard(sleep: SleepSummary) {
    val colors = LocalPulseColors.current
    val score = SleepScore.calculate(sleep)
    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${score.score}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                Text(score.label.label, fontSize = 11.sp, color = colors.textMuted)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(formatSleepDuration(sleep.totalMinutes), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                Text("${timeFmt.format(java.util.Date(sleep.session.startAt))} to ${timeFmt.format(java.util.Date(sleep.session.endAt))}", fontSize = 13.sp, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun StageTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.textPrimary)
            Text(label, fontSize = 11.sp, color = colors.textMuted)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    val colors = LocalPulseColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.background(color, RoundedCornerShape(50)).padding(3.dp))
        Text(label, fontSize = 10.sp, color = colors.textSecondary)
    }
}

@Composable
private fun RangeSelector(
    selected: SleepRangeKey,
    selectedDate: Long?,
    onSelect: (SleepRangeKey) -> Unit,
    onDateSelected: (Long?) -> Unit,
) {
    val colors = LocalPulseColors.current
    val options = listOf(SleepRangeKey.DAY to stringResource(R.string.sleep_range_night), SleepRangeKey.WEEK to stringResource(R.string.sleep_range_week), SleepRangeKey.MONTH to stringResource(R.string.sleep_range_month), SleepRangeKey.YEAR to stringResource(R.string.sleep_range_year))
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
                    Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) androidx.compose.ui.graphics.Color.White else colors.textSecondary)
                }
            }
        }
        CalendarIconButton(selectedDateMs = selectedDate, onDateSelected = onDateSelected)
    }
}
