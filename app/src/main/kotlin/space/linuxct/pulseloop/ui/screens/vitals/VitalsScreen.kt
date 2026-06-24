package space.linuxct.pulseloop.ui.screens.vitals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.ui.charts.HrLineChart
import space.linuxct.pulseloop.ui.charts.Spo2LineChart
import space.linuxct.pulseloop.ui.charts.MetricRangeChart
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.VitalsViewModel

@Composable
fun VitalsScreen(vm: VitalsViewModel = hiltViewModel()) {
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
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp + navBarPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column {
                    Text("Vitals", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Live measurements and trends", fontSize = 14.sp, color = colors.textSecondary)
                }
            }

            // Heart rate card
            item {
                VitalsDetailCard(title = "Heart rate", accentColor = colors.heartRate) {
                    val label = hrRangeLabel(state.hrSamples.map { it.value }, state.latestHR)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        if (state.hrSamples.isNotEmpty() || state.latestHR != null) {
                            Text("bpm range", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (state.isMeasuringHR) {
                            Spacer(modifier = Modifier.weight(1f))
                            PulsingDot(color = colors.heartRate)
                        }
                    }
                    if (state.isMeasuringHR) {
                        Text(
                            "Measuring… keep the ring still",
                            fontSize = 12.sp, color = colors.heartRate,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        "Resting estimate: ${state.restingHREstimate?.let { "${it.toInt()}" } ?: "Calibrating"}  ·  " +
                        "Peak today: ${state.peakHRToday?.let { "${it.toInt()}" } ?: "Not enough data"}",
                        fontSize = 12.sp, color = colors.textMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        state.hrSamples.size > 1 -> HrLineChart(
                            samples = state.hrSamples,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                        state.hrSamples.size == 1 -> InlineEmptyState("First reading recorded", "Sync again later to build your trend.")
                        else -> InlineEmptyState("No HR samples yet", "Wear the ring and sync to start your trend.")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val isConnected = state.connectionState == RingConnectionState.CONNECTED
                    if (state.isMeasuringHR) {
                        SecondaryButton(
                            title = "Stop measurement",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.stopHRMeasurement()
                            }
                        )
                    } else {
                        PrimaryButton(
                            title = "Measure HR",
                            enabled = isConnected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.startHRMeasurement()
                            }
                        )
                        if (!isConnected) {
                            Text(
                                "Ring not connected",
                                fontSize = 11.sp,
                                color = colors.textMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // SpO2 card
            item {
                VitalsDetailCard(title = "Blood oxygen", accentColor = colors.spo2) {
                    val label = averageLabel(state.spo2Samples.map { it.value }, state.latestSpO2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        if (state.spo2Samples.isNotEmpty() || state.latestSpO2 != null) {
                            Text("% avg", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        if (state.isMeasuringSpO2) {
                            Spacer(modifier = Modifier.weight(1f))
                            PulsingDot(color = colors.spo2)
                        }
                    }

                    if (state.isMeasuringSpO2) {
                        Text(
                            "Measuring… keep the ring still",
                            fontSize = 12.sp, color = colors.spo2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        state.spo2Samples.size > 1 -> Spo2LineChart(
                            samples = state.spo2Samples,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                        state.spo2Samples.size == 1 -> InlineEmptyState("First reading recorded", "Measure again later to build your trend.")
                        else -> InlineEmptyState("No SpO₂ samples yet", "Tap Measure to take a reading.")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val isConnected = state.connectionState == RingConnectionState.CONNECTED
                    if (state.isMeasuringSpO2) {
                        SecondaryButton(
                            title = "Stop measurement",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.stopSpO2Measurement()
                            }
                        )
                    } else {
                        PrimaryButton(
                            title = "Measure SpO₂",
                            enabled = isConnected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.startSpO2Measurement()
                            }
                        )
                        if (!isConnected) {
                            Text(
                                "Ring not connected",
                                fontSize = 11.sp,
                                color = colors.textMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Stress card (capability-gated)
            if (state.capabilities.contains(WearableCapability.STRESS)) {
                item {
                    VitalsDetailCard(title = "Stress", accentColor = colors.stress) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.stressSamples.isNotEmpty()) {
                            MetricRangeChart(
                                samples = state.stressSamples,
                                modifier = Modifier.fillMaxWidth(),
                                height = 120
                            )
                        } else {
                            InlineEmptyState("No stress data yet", "Wear the ring through the day and sync.")
                        }
                    }
                }
            }

            // HRV card (capability-gated)
            if (state.capabilities.contains(WearableCapability.HRV)) {
                item {
                    VitalsDetailCard(title = "HRV", accentColor = colors.hrv) {
                        val label = state.hrvSamples.lastOrNull()?.value?.let { "${it.toInt()}" } ?: "--"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.hrvSamples.isNotEmpty()) {
                                Text("ms", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(top = 20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.hrvSamples.size > 1) {
                            MetricRangeChart(
                                samples = state.hrvSamples,
                                modifier = Modifier.fillMaxWidth(),
                                height = 120
                            )
                        } else {
                            InlineEmptyState("No HRV data yet", "HRV builds up over a few hours of wear.")
                        }
                    }
                }
            }

            // Temperature card (capability-gated)
            if (state.capabilities.contains(WearableCapability.TEMPERATURE)) {
                item {
                    VitalsDetailCard(title = "Skin temperature", accentColor = colors.temperature) {
                        val label = state.tempSamples.lastOrNull()?.value?.let { "%.1f".format(it) } ?: "--"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(label, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            if (state.tempSamples.isNotEmpty()) {
                                Text("°C", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textMuted, modifier = Modifier.padding(top = 20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.tempSamples.size > 1) {
                            MetricRangeChart(
                                samples = state.tempSamples,
                                modifier = Modifier.fillMaxWidth(),
                                height = 120
                            )
                        } else {
                            InlineEmptyState("No temperature data yet", "Temperature trends appear after overnight wear.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "spo2_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "spo2_alpha"
    )
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .size(10.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
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
                        .background(accentColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
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
        Text(message, fontSize = 12.sp, color = colors.textMuted)
    }
}

private fun hrRangeLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    val lo = values.min().toInt(); val hi = values.max().toInt()
    return if (lo == hi) "$lo" else "$lo-$hi"
}

private fun averageLabel(samples: List<Double>, fallback: Double?): String {
    val values = samples.filter { it > 0 }
    if (values.isEmpty()) return fallback?.let { it.toInt().toString() } ?: "—"
    return (values.sum() / values.size).toInt().toString()
}
