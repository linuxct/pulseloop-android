package space.linuxct.pulseloop.ui.screens.vitals.v2

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.VitalsBar
import space.linuxct.pulseloop.domain.model.VitalsRangeKey
import space.linuxct.pulseloop.domain.model.WearableCapability
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import space.linuxct.pulseloop.ui.charts.BloodPressureRangeChart
import space.linuxct.pulseloop.ui.charts.HrLineChart
import space.linuxct.pulseloop.ui.charts.MetricLineChart
import space.linuxct.pulseloop.ui.charts.MetricRangeChart
import space.linuxct.pulseloop.ui.charts.Spo2LineChart
import space.linuxct.pulseloop.ui.charts.VitalsBarsChart
import space.linuxct.pulseloop.ui.charts.bloodPressureZone
import space.linuxct.pulseloop.ui.charts.color
import space.linuxct.pulseloop.ui.charts.glucoseZone
import space.linuxct.pulseloop.ui.charts.labelRes
import space.linuxct.pulseloop.ui.charts.lowerIsBetterBand
import space.linuxct.pulseloop.ui.components.CalendarIconButton
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.components.LargeScreenTitle
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.screens.vitals.PulsingDot
import space.linuxct.pulseloop.ui.screens.vitals.averageLabel
import space.linuxct.pulseloop.ui.screens.vitals.hrRangeLabel
import space.linuxct.pulseloop.ui.viewmodel.VitalsViewModel

@Composable
internal fun VitalsScreenV2(vm: VitalsViewModel) {
    val state by vm.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scheme = MaterialTheme.colorScheme
    val pulseColors = LocalPulseColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 96.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { LargeScreenTitle(title = stringResource(R.string.screen_title_vitals), subtitle = stringResource(R.string.vitals_screen_subtitle)) }

            item {
                val options = listOf(
                    VitalsRangeKey.DAY   to stringResource(R.string.vitals_range_day),
                    VitalsRangeKey.WEEK  to stringResource(R.string.vitals_range_week),
                    VitalsRangeKey.MONTH to stringResource(R.string.vitals_range_month),
                    VitalsRangeKey.YEAR  to stringResource(R.string.vitals_range_year),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        options.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = state.vitalsRange == key && state.selectedDate == null,
                                onClick = { vm.setVitalsRange(key); vm.setSelectedDate(null) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                    CalendarIconButton(
                        selectedDateMs = state.selectedDate,
                        onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setVitalsRange(VitalsRangeKey.DAY) }
                    )
                }
            }

            item {
                VitalsCardV2(title = stringResource(R.string.vitals_card_heart_rate), accentColor = scheme.error) {
                    if (state.vitalsRange == VitalsRangeKey.DAY) {
                        val label = hrRangeLabel(state.hrSamples.map { it.value }, state.latestHR)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (state.hrSamples.isNotEmpty() || state.latestHR != null) {
                                Text(stringResource(R.string.vitals_hr_unit_label), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            if (state.isMeasuringHR) {
                                Spacer(modifier = Modifier.weight(1f))
                                PulsingDot(color = scheme.primary)
                            }
                        }
                        if (state.isMeasuringHR) Text(stringResource(R.string.vitals_measuring_hint), style = MaterialTheme.typography.bodySmall, color = scheme.primary, modifier = Modifier.padding(top = 2.dp))
                        val restingEst = state.restingHREstimate
                        val peakHR = state.peakHRToday
                        val restingText = if (restingEst != null) stringResource(R.string.vitals_hr_resting_label_v2, "${restingEst.toInt()} bpm") else stringResource(R.string.vitals_hr_calibrating)
                        val peakText = if (peakHR != null) stringResource(R.string.vitals_hr_peak_label_v2, "${peakHR.toInt()} bpm") else "—"
                        Text(
                            "$restingText  ·  $peakText",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            state.hrSamples.size > 1  -> HrLineChart(samples = state.hrSamples, modifier = Modifier.fillMaxWidth().height(140.dp))
                            state.hrSamples.size == 1 -> EmptyStateV2(stringResource(R.string.vitals_hr_first_reading_title), stringResource(R.string.vitals_hr_first_reading_message))
                            else                      -> EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_hr_no_samples_title_past else R.string.vitals_hr_no_samples_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_hr_no_samples_message))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val isConnected = state.connectionState == RingConnectionState.CONNECTED
                        if (state.isMeasuringHR) {
                            SecondaryButton(title = stringResource(R.string.action_stop_measurement), compact = true, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopHRMeasurement() })
                        } else {
                            PrimaryButton(title = stringResource(R.string.action_measure_hr), enabled = isConnected, compact = true, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startHRMeasurement() })
                            if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        VitalsHistoryContentV2(
                            avg = state.hrRangeAvg,
                            min = state.hrRangeMin,
                            max = state.hrRangeMax,
                            bars = state.hrBars,
                            accentColor = scheme.error,
                            unit = stringResource(R.string.unit_bpm),
                        )
                    }
                }
            }

            item {
                VitalsCardV2(title = stringResource(R.string.vitals_card_blood_oxygen), accentColor = scheme.tertiary) {
                    if (state.vitalsRange == VitalsRangeKey.DAY) {
                        val label = averageLabel(state.spo2Samples.map { it.value }, state.latestSpO2)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (state.spo2Samples.isNotEmpty() || state.latestSpO2 != null) {
                                Text(stringResource(R.string.vitals_spo2_unit_label), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            if (state.isMeasuringSpO2) {
                                Spacer(modifier = Modifier.weight(1f))
                                PulsingDot(color = scheme.primary)
                            }
                        }
                        if (state.isMeasuringSpO2) Text(stringResource(R.string.vitals_measuring_hint), style = MaterialTheme.typography.bodySmall, color = scheme.primary, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            state.spo2Samples.size > 1  -> Spo2LineChart(samples = state.spo2Samples, modifier = Modifier.fillMaxWidth().height(140.dp))
                            state.spo2Samples.size == 1 -> EmptyStateV2(stringResource(R.string.vitals_spo2_first_reading_title), stringResource(R.string.vitals_spo2_first_reading_message))
                            else                        -> EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_spo2_no_samples_title_past else R.string.vitals_spo2_no_samples_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_spo2_no_samples_message))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val isConnected = state.connectionState == RingConnectionState.CONNECTED
                        if (state.isMeasuringSpO2) {
                            SecondaryButton(title = stringResource(R.string.action_stop_measurement), compact = true, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                        } else {
                            PrimaryButton(title = stringResource(R.string.action_measure_spo2), enabled = isConnected, compact = true, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startSpO2Measurement() })
                            if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        VitalsHistoryContentV2(
                            avg = state.spo2RangeAvg,
                            min = state.spo2RangeMin,
                            max = state.spo2RangeMax,
                            bars = state.spo2Bars,
                            accentColor = scheme.tertiary,
                            unit = stringResource(R.string.unit_percent),
                        )
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.STRESS)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_stress), accentColor = scheme.secondary) {
                        val stress = state.latestStress
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                            Text(stress?.let { "${it.toInt()}" } ?: "--", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (stress != null) Text(stringResource(R.string.vitals_score_out_of_100), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (stress != null) Text(stringResource(lowerIsBetterBand(stress.toInt())), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Text(stringResource(R.string.vitals_stress_explainer), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.stressSamples.size > 1) MetricLineChart(samples = state.stressSamples, color = pulseColors.stress, modifier = Modifier.fillMaxWidth(), height = 120)
                        else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_stress_no_data_title_past else R.string.vitals_stress_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_stress_no_data_message))
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.HRV)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_hrv), accentColor = scheme.secondary) {
                        val label = state.hrvSamples.lastOrNull()?.value?.let { "${it.toInt()}" } ?: "--"
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (state.hrvSamples.isNotEmpty()) Text(stringResource(R.string.unit_ms), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.hrvSamples.size > 1) MetricRangeChart(samples = state.hrvSamples, modifier = Modifier.fillMaxWidth(), height = 120)
                        else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_hrv_no_data_title_past else R.string.vitals_hrv_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_hrv_no_data_message))
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.TEMPERATURE)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_skin_temperature), accentColor = scheme.secondary) {
                        val label = state.tempSamples.lastOrNull()?.value?.let { "%.1f".format(it) } ?: "--"
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Text(label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (state.tempSamples.isNotEmpty()) Text(stringResource(R.string.unit_celsius), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.tempSamples.size > 1) MetricRangeChart(samples = state.tempSamples, modifier = Modifier.fillMaxWidth(), height = 120)
                        else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_temp_no_data_title_past else R.string.vitals_temp_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_temp_no_data_message))
                    }
                }
            }

            // ── Blood pressure (Jring combined measurement; opt-in) ─────────────
            if (state.bloodMetricsEnabled && state.capabilities.contains(WearableCapability.BLOOD_PRESSURE)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_blood_pressure), accentColor = pulseColors.bloodPressure) {
                        val sys = state.latestBpSys
                        val dia = state.latestBpDia
                        val bpZone = bloodPressureZone(sys, dia)
                        val valueColor = bpZone?.color(pulseColors) ?: scheme.onSurface
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                            Text(if (sys != null || dia != null) "${sys?.toInt() ?: "--"} / ${dia?.toInt() ?: "--"}" else "--", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
                            if (sys != null || dia != null) Text(stringResource(R.string.unit_mmhg), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (bpZone != null) Text(stringResource(bpZone.labelRes()), style = MaterialTheme.typography.bodySmall, color = valueColor, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.bpSysSamples.size > 1) {
                            BloodPressureRangeChart(systolic = state.bpSysSamples, diastolic = state.bpDiaSamples, systolicColor = pulseColors.bloodPressure, diastolicColor = pulseColors.bloodPressure.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(), height = 120)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                LegendDotV2(pulseColors.bloodPressure, stringResource(R.string.vitals_bp_systolic_legend))
                                LegendDotV2(pulseColors.bloodPressure.copy(alpha = 0.5f), stringResource(R.string.vitals_bp_diastolic_legend))
                            }
                        } else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_bp_no_data_title_past else R.string.vitals_bp_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_bp_no_data_message))
                        CombinedMeasureButtonV2(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }

            // ── Blood sugar (Jring combined measurement; opt-in) ────────────────
            if (state.bloodMetricsEnabled && state.capabilities.contains(WearableCapability.BLOOD_SUGAR)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_blood_sugar), accentColor = pulseColors.bloodSugar) {
                        val glucose = state.latestGlucose
                        val glucoseZ = glucoseZone(glucose)
                        val valueColor = glucoseZ?.color(pulseColors) ?: scheme.onSurface
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                            Text(glucose?.let { "%.0f".format(it) } ?: "--", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
                            if (glucose != null) Text(stringResource(R.string.unit_mgdl), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (glucoseZ != null) Text(stringResource(glucoseZ.labelRes()), style = MaterialTheme.typography.bodySmall, color = valueColor, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.glucoseSamples.size > 1) MetricLineChart(samples = state.glucoseSamples, color = pulseColors.bloodSugar, unit = stringResource(R.string.unit_mgdl), modifier = Modifier.fillMaxWidth(), height = 120)
                        else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_glucose_no_data_title_past else R.string.vitals_glucose_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_glucose_no_data_message))
                        CombinedMeasureButtonV2(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }

            // ── Fatigue (Jring combined measurement) ────────────────────────────
            if (state.capabilities.contains(WearableCapability.FATIGUE)) {
                item {
                    VitalsCardV2(title = stringResource(R.string.vitals_card_fatigue), accentColor = pulseColors.fatigue) {
                        val fatigue = state.latestFatigue
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                            Text(fatigue?.let { "${it.toInt()}" } ?: "--", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            if (fatigue != null) Text(stringResource(R.string.vitals_score_out_of_100), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (fatigue != null) Text(stringResource(lowerIsBetterBand(fatigue.toInt())), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Text(stringResource(R.string.vitals_fatigue_explainer), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.fatigueSamples.size > 1) MetricLineChart(samples = state.fatigueSamples, color = pulseColors.fatigue, modifier = Modifier.fillMaxWidth(), height = 120)
                        else EmptyStateV2(stringResource(if (state.selectedDate != null) R.string.vitals_fatigue_no_data_title_past else R.string.vitals_fatigue_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_fatigue_no_data_message))
                        CombinedMeasureButtonV2(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDotV2(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CombinedMeasureButtonV2(
    isMeasuring: Boolean,
    isConnected: Boolean,
    onMeasure: () -> Unit,
    onStop: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Spacer(modifier = Modifier.height(12.dp))
    if (isMeasuring) {
        Text(stringResource(R.string.vitals_measuring_hint), style = MaterialTheme.typography.bodySmall, color = scheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        SecondaryButton(title = stringResource(R.string.action_stop_measurement), compact = true, onClick = onStop)
    } else {
        PrimaryButton(title = stringResource(R.string.vitals_measure_action), enabled = isConnected, compact = true, onClick = onMeasure)
        if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun VitalsHistoryContentV2(
    avg: Double?,
    min: Double?,
    max: Double?,
    bars: List<VitalsBar>,
    accentColor: androidx.compose.ui.graphics.Color,
    unit: String,
) {
    val scheme = MaterialTheme.colorScheme
    if (avg != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                stringResource(R.string.vitals_history_avg_label, "") to avg,
                stringResource(R.string.vitals_history_min_label, "") to min,
                stringResource(R.string.vitals_history_max_label, "") to max,
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label.trim(), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(
                        if (value != null) "${value.toInt()} $unit" else "--",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    VitalsBarsChart(bars = bars, accentColor = accentColor, unit = unit, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun VitalsCardV2(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp, 20.dp).background(accentColor, RoundedCornerShape(2.dp)))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            content()
        }
    }
}

@Composable
private fun EmptyStateV2(title: String, message: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
