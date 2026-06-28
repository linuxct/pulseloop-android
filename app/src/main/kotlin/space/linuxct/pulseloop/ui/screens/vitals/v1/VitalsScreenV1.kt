package space.linuxct.pulseloop.ui.screens.vitals.v1

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.VitalsRangeKey
import space.linuxct.pulseloop.domain.model.WearableCapability
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.screens.vitals.PulsingDot
import space.linuxct.pulseloop.ui.screens.vitals.averageLabel
import space.linuxct.pulseloop.ui.screens.vitals.hrRangeLabel
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.VitalsViewModel

@Composable
internal fun VitalsScreenV1(vm: VitalsViewModel) {
    val colors = LocalPulseColors.current
    val state by vm.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text(stringResource(R.string.screen_title_vitals), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(stringResource(R.string.vitals_screen_subtitle), fontSize = 14.sp, color = colors.textMuted)
                }
            }

            item {
                VitalsRangeSelectorV1(
                    selected = state.vitalsRange,
                    selectedDate = state.selectedDate,
                    onSelect = { vm.setVitalsRange(it) },
                    onDateSelected = { ms -> vm.setSelectedDate(ms); if (ms != null) vm.setVitalsRange(VitalsRangeKey.DAY) }
                )
            }

            item {
                VitalsDetailCard(title = stringResource(R.string.vitals_card_heart_rate), accentColor = colors.heartRate) {
                    if (state.vitalsRange == VitalsRangeKey.DAY) {
                        val label = hrRangeLabel(state.hrSamples.map { it.value }, state.latestHR)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.hrSamples.isNotEmpty() || state.latestHR != null) {
                                Text(stringResource(R.string.vitals_hr_unit_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            if (state.isMeasuringHR) {
                                Spacer(modifier = Modifier.weight(1f))
                                PulsingDot(color = colors.heartRate)
                            }
                        }
                        if (state.isMeasuringHR) Text(stringResource(R.string.vitals_measuring_hint), fontSize = 12.sp, color = colors.heartRate, modifier = Modifier.padding(top = 4.dp))
                        val restingEst = state.restingHREstimate
                        val peakHR = state.peakHRToday
                        val restingText = if (restingEst != null) stringResource(R.string.vitals_hr_resting_label, "${restingEst.toInt()}") else stringResource(R.string.vitals_hr_calibrating)
                        val peakText = if (peakHR != null) stringResource(R.string.vitals_hr_peak_label, "${peakHR.toInt()}") else stringResource(R.string.vitals_hr_not_enough_data)
                        Text(
                            "$restingText  ·  $peakText",
                            fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            state.hrSamples.size > 1  -> HrLineChart(samples = state.hrSamples, modifier = Modifier.fillMaxWidth().height(140.dp))
                            state.hrSamples.size == 1 -> InlineEmptyState(stringResource(R.string.vitals_hr_first_reading_title), stringResource(R.string.vitals_hr_first_reading_message))
                            else                      -> InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_hr_no_samples_title_past else R.string.vitals_hr_no_samples_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_hr_no_samples_message))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val isConnected = state.connectionState == RingConnectionState.CONNECTED
                        if (state.isMeasuringHR) {
                            SecondaryButton(title = stringResource(R.string.action_stop_measurement), onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopHRMeasurement() })
                        } else {
                            PrimaryButton(title = stringResource(R.string.action_measure_hr), enabled = isConnected, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startHRMeasurement() })
                            if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        VitalsHistoryContent(
                            avg = state.hrRangeAvg,
                            min = state.hrRangeMin,
                            max = state.hrRangeMax,
                            bars = state.hrBars,
                            accentColor = colors.heartRate,
                            unit = stringResource(R.string.unit_bpm),
                        )
                    }
                }
            }

            item {
                VitalsDetailCard(title = stringResource(R.string.vitals_card_blood_oxygen), accentColor = colors.spo2) {
                    if (state.vitalsRange == VitalsRangeKey.DAY) {
                        val label = averageLabel(state.spo2Samples.map { it.value }, state.latestSpO2)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.spo2Samples.isNotEmpty() || state.latestSpO2 != null) {
                                Text(stringResource(R.string.vitals_spo2_unit_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            if (state.isMeasuringSpO2) {
                                Spacer(modifier = Modifier.weight(1f))
                                PulsingDot(color = colors.spo2)
                            }
                        }
                        if (state.isMeasuringSpO2) Text(stringResource(R.string.vitals_measuring_hint), fontSize = 12.sp, color = colors.spo2, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            state.spo2Samples.size > 1  -> Spo2LineChart(samples = state.spo2Samples, modifier = Modifier.fillMaxWidth().height(140.dp))
                            state.spo2Samples.size == 1 -> InlineEmptyState(stringResource(R.string.vitals_spo2_first_reading_title), stringResource(R.string.vitals_spo2_first_reading_message))
                            else                        -> InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_spo2_no_samples_title_past else R.string.vitals_spo2_no_samples_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_spo2_no_samples_message))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val isConnected = state.connectionState == RingConnectionState.CONNECTED
                        if (state.isMeasuringSpO2) {
                            SecondaryButton(title = stringResource(R.string.action_stop_measurement), onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                        } else {
                            PrimaryButton(title = stringResource(R.string.action_measure_spo2), enabled = isConnected, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startSpO2Measurement() })
                            if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        VitalsHistoryContent(
                            avg = state.spo2RangeAvg,
                            min = state.spo2RangeMin,
                            max = state.spo2RangeMax,
                            bars = state.spo2Bars,
                            accentColor = colors.spo2,
                            unit = stringResource(R.string.unit_percent),
                        )
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.STRESS)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_stress), accentColor = colors.stress) {
                        val stress = state.latestStress
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stress?.let { "${it.toInt()}" } ?: "--", fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (stress != null) Text(stringResource(R.string.vitals_score_out_of_100), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (stress != null) Text(stringResource(lowerIsBetterBand(stress.toInt())), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        Text(stringResource(R.string.vitals_stress_explainer), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.stressSamples.size > 1) MetricLineChart(samples = state.stressSamples, color = colors.stress, modifier = Modifier.fillMaxWidth(), height = 120)
                        else InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_stress_no_data_title_past else R.string.vitals_stress_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_stress_no_data_message))
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.HRV)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_hrv), accentColor = colors.hrv) {
                        val label = state.hrvSamples.lastOrNull()?.value?.let { "${it.toInt()}" } ?: "--"
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.hrvSamples.isNotEmpty()) Text(stringResource(R.string.unit_ms), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(top = 20.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.hrvSamples.size > 1) MetricRangeChart(samples = state.hrvSamples, modifier = Modifier.fillMaxWidth(), height = 120)
                        else InlineEmptyState(stringResource(R.string.vitals_hrv_no_data_title), stringResource(R.string.vitals_hrv_no_data_message))
                    }
                }
            }

            if (state.capabilities.contains(WearableCapability.TEMPERATURE)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_skin_temperature), accentColor = colors.temperature) {
                        val label = state.tempSamples.lastOrNull()?.value?.let { "%.1f".format(it) } ?: "--"
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.tempSamples.isNotEmpty()) Text(stringResource(R.string.unit_celsius), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(top = 20.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.tempSamples.size > 1) MetricRangeChart(samples = state.tempSamples, modifier = Modifier.fillMaxWidth(), height = 120)
                        else InlineEmptyState(stringResource(R.string.vitals_temp_no_data_title), stringResource(R.string.vitals_temp_no_data_message))
                    }
                }
            }

            // ── Blood pressure (Jring combined measurement; opt-in) ─────────────
            if (state.bloodMetricsEnabled && state.capabilities.contains(WearableCapability.BLOOD_PRESSURE)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_blood_pressure), accentColor = colors.bloodPressure) {
                        val sys = state.latestBpSys
                        val dia = state.latestBpDia
                        val bpZone = bloodPressureZone(sys, dia)
                        val valueColor = bpZone?.color(colors) ?: colors.textPrimary
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                            Text(if (sys != null || dia != null) "${sys?.toInt() ?: "--"} / ${dia?.toInt() ?: "--"}" else "--", fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
                            if (sys != null || dia != null) Text(stringResource(R.string.unit_mmhg), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (bpZone != null) Text(stringResource(bpZone.labelRes()), fontSize = 12.sp, color = valueColor, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.bpSysSamples.size > 1) {
                            BloodPressureRangeChart(systolic = state.bpSysSamples, diastolic = state.bpDiaSamples, systolicColor = colors.bloodPressure, diastolicColor = colors.bloodPressure.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(), height = 120)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                LegendDot(colors.bloodPressure, stringResource(R.string.vitals_bp_systolic_legend))
                                LegendDot(colors.bloodPressure.copy(alpha = 0.5f), stringResource(R.string.vitals_bp_diastolic_legend))
                            }
                        } else InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_bp_no_data_title_past else R.string.vitals_bp_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_bp_no_data_message))
                        CombinedMeasureButton(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }

            // ── Blood sugar (Jring combined measurement; opt-in) ────────────────
            if (state.bloodMetricsEnabled && state.capabilities.contains(WearableCapability.BLOOD_SUGAR)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_blood_sugar), accentColor = colors.bloodSugar) {
                        val glucose = state.latestGlucose
                        val glucoseZ = glucoseZone(glucose)
                        val valueColor = glucoseZ?.color(colors) ?: colors.textPrimary
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                            Text(glucose?.let { "%.0f".format(it) } ?: "--", fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
                            if (glucose != null) Text(stringResource(R.string.unit_mgdl), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (glucoseZ != null) Text(stringResource(glucoseZ.labelRes()), fontSize = 12.sp, color = valueColor, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.glucoseSamples.size > 1) MetricLineChart(samples = state.glucoseSamples, color = colors.bloodSugar, unit = stringResource(R.string.unit_mgdl), modifier = Modifier.fillMaxWidth(), height = 120)
                        else InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_glucose_no_data_title_past else R.string.vitals_glucose_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_glucose_no_data_message))
                        CombinedMeasureButton(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }

            // ── Fatigue (Jring combined measurement) ────────────────────────────
            if (state.capabilities.contains(WearableCapability.FATIGUE)) {
                item {
                    VitalsDetailCard(title = stringResource(R.string.vitals_card_fatigue), accentColor = colors.fatigue) {
                        val fatigue = state.latestFatigue
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 12.dp)) {
                            Text(fatigue?.let { "${it.toInt()}" } ?: "--", fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (fatigue != null) Text(stringResource(R.string.vitals_score_out_of_100), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (fatigue != null) Text(stringResource(lowerIsBetterBand(fatigue.toInt())), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        Text(stringResource(R.string.vitals_fatigue_explainer), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.fatigueSamples.size > 1) MetricLineChart(samples = state.fatigueSamples, color = colors.fatigue, modifier = Modifier.fillMaxWidth(), height = 120)
                        else InlineEmptyState(stringResource(if (state.selectedDate != null) R.string.vitals_fatigue_no_data_title_past else R.string.vitals_fatigue_no_data_title), stringResource(if (state.selectedDate != null) R.string.vitals_no_data_past_message else R.string.vitals_fatigue_no_data_message))
                        CombinedMeasureButton(state.isMeasuringSpO2, state.connectionState == RingConnectionState.CONNECTED, onMeasure = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.startCombinedMeasurement() }, onStop = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.stopSpO2Measurement() })
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = LocalPulseColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = colors.textMuted)
    }
}

@Composable
private fun CombinedMeasureButton(
    isMeasuring: Boolean,
    isConnected: Boolean,
    onMeasure: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalPulseColors.current
    Spacer(modifier = Modifier.height(12.dp))
    if (isMeasuring) {
        Text(stringResource(R.string.vitals_measuring_hint), fontSize = 12.sp, color = colors.textMuted, modifier = Modifier.padding(bottom = 8.dp))
        SecondaryButton(title = stringResource(R.string.action_stop_measurement), onClick = onStop)
    } else {
        PrimaryButton(title = stringResource(R.string.vitals_measure_action), enabled = isConnected, onClick = onMeasure)
        if (!isConnected) Text(stringResource(R.string.vitals_ring_not_connected), fontSize = 11.sp, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun VitalsRangeSelectorV1(
    selected: VitalsRangeKey,
    selectedDate: Long?,
    onSelect: (VitalsRangeKey) -> Unit,
    onDateSelected: (Long?) -> Unit,
) {
    val colors = LocalPulseColors.current
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

@Composable
private fun VitalsHistoryContent(
    avg: Double?,
    min: Double?,
    max: Double?,
    bars: List<space.linuxct.pulseloop.domain.model.VitalsBar>,
    accentColor: androidx.compose.ui.graphics.Color,
    unit: String,
) {
    val colors = LocalPulseColors.current
    if (avg != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                stringResource(R.string.vitals_history_avg_label, "") to avg,
                stringResource(R.string.vitals_history_min_label, "") to min,
                stringResource(R.string.vitals_history_max_label, "") to max,
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label.trim(), fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                    Text(
                        if (value != null) "${value.toInt()} $unit" else "--",
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    VitalsBarsChart(bars = bars, accentColor = accentColor, unit = unit, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun VitalsDetailCard(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    val colors = LocalPulseColors.current
    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .padding(top = 3.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                        .padding(horizontal = 3.dp)
                )
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
            }
            content()
        }
    }
}

@Composable
private fun InlineEmptyState(title: String, message: String) {
    val colors = LocalPulseColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = colors.textPrimary)
        if (message.isNotBlank()) Text(message, fontSize = 12.sp, color = colors.textMuted)
    }
}
