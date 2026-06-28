package space.linuxct.pulseloop.ui.screens.sleep.v2

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.SleepRangeKey
import space.linuxct.pulseloop.domain.service.SleepInsights
import space.linuxct.pulseloop.domain.service.SleepScore
import space.linuxct.pulseloop.ui.charts.SleepBarsChart
import space.linuxct.pulseloop.ui.charts.SleepTimelineChart
import space.linuxct.pulseloop.ui.components.CalendarIconButton
import space.linuxct.pulseloop.ui.components.LargeScreenTitle
import space.linuxct.pulseloop.ui.screens.sleep.formatSleepDuration
import space.linuxct.pulseloop.ui.viewmodel.SleepViewModel

@Composable
internal fun SleepScreenV2(vm: SleepViewModel) {
    val state by vm.uiState.collectAsState()
    val range = state.range
    val rangeSummary = state.rangeSummary
    val validSessions = rangeSummary?.let { SleepInsights.validSessions(it.sessions) } ?: emptyList()
    val lastNight = if (range == SleepRangeKey.DAY) validSessions.lastOrNull() else null
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(scheme.background)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 80.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { LargeScreenTitle(title = stringResource(R.string.screen_title_sleep), subtitle = stringResource(R.string.sleep_screen_subtitle_v2)) }

            item {
                val options = listOf(SleepRangeKey.DAY to stringResource(R.string.sleep_range_night), SleepRangeKey.WEEK to stringResource(R.string.sleep_range_week), SleepRangeKey.MONTH to stringResource(R.string.sleep_range_month), SleepRangeKey.YEAR to stringResource(R.string.sleep_range_year))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        options.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = range == key && state.selectedDate == null,
                                onClick = { vm.setRange(key); vm.setSelectedDate(null) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                    CalendarIconButton(
                        selectedDateMs = state.selectedDate,
                        onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setRange(SleepRangeKey.DAY) }
                    )
                }
            }

            if (range == SleepRangeKey.DAY) {
                if (lastNight != null) {
                    item {
                        val score = SleepScore.calculate(lastNight)
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer), elevation = CardDefaults.cardElevation(0.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${score.score}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                                    Text(score.label.label, style = MaterialTheme.typography.labelSmall, color = scheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(formatSleepDuration(lastNight.totalMinutes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = scheme.onPrimaryContainer)
                                    val timeFmt = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                    Text("${timeFmt.format(java.util.Date(lastNight.session.startAt))} – ${timeFmt.format(java.util.Date(lastNight.session.endAt))}", style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.sleep_stages_header), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LegendDotV2(stringResource(R.string.sleep_stage_deep), scheme.primary)
                                        LegendDotV2(stringResource(R.string.sleep_stage_light), scheme.secondary)
                                        LegendDotV2(stringResource(R.string.sleep_stage_awake), scheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (lastNight.blocks.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) { SleepTimelineChart(sleep = lastNight) }
                                } else {
                                    Text(stringResource(R.string.sleep_no_stage_data), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    StageTileV2(stringResource(R.string.sleep_stage_deep), formatSleepDuration(lastNight.deepMinutes), scheme.primary, Modifier.weight(1f))
                                    StageTileV2(stringResource(R.string.sleep_stage_light), formatSleepDuration(lastNight.lightMinutes), scheme.secondary, Modifier.weight(1f))
                                    StageTileV2(stringResource(R.string.sleep_stage_awake), formatSleepDuration(lastNight.awakeMinutes), scheme.error, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        val score = SleepScore.calculate(lastNight)
                        val coach = SleepInsights.dayCoach(lastNight, score.score, score.awakePct, score.deepPct, null)
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(coach.headline, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text(coach.body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    coach.chips.forEach { chip -> SuggestionChip(onClick = {}, label = { Text(chip, style = MaterialTheme.typography.labelSmall) }) }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                                Text(stringResource(if (state.selectedDate != null) R.string.sleep_no_sleep_title_past else R.string.sleep_no_sleep_title_v2), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                                Text(stringResource(if (state.selectedDate != null) R.string.sleep_no_sleep_message_past else R.string.sleep_no_sleep_message), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                val avgMin = SleepInsights.averageDuration(validSessions)
                val avgScore = SleepInsights.averageScore(validSessions)
                val enough = validSessions.size >= 2

                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(SleepInsights.rangeHeroLabel[range] ?: "Sleep", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            Text(if (enough && avgMin != null) "${formatSleepDuration(avgMin)} avg" else "Not enough data", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (avgScore != null && enough) Text("Score: $avgScore · ${SleepScore.qualityLabel(avgScore).label}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            Text("${validSessions.size} of ${rangeSummary?.expectedNights ?: "?"} nights tracked", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }

                if (state.bars.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(stringResource(R.string.sleep_duration_chart_title), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) { SleepBarsChart(bars = state.bars, goalMinutes = state.goalMinutes) }
                            }
                        }
                    }
                }

                item {
                    val coach = SleepInsights.aggregateCoach(range, rangeSummary?.sessions ?: emptyList(), rangeSummary?.expectedNights ?: 0, state.goalMinutes)
                    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(coach.headline, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(coach.body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                coach.chips.forEach { chip -> SuggestionChip(onClick = {}, label = { Text(chip, style = MaterialTheme.typography.labelSmall) }) }
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
private fun StageTileV2(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(3.dp).fillMaxWidth().background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendDotV2(label: String, color: androidx.compose.ui.graphics.Color) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.background(color, RoundedCornerShape(50)).padding(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}
