package space.linuxct.pulseloop.ui.screens.settings.v1

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.settings.formatFirmwareVersion
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.SettingsUiState
import space.linuxct.pulseloop.ui.viewmodel.SettingsViewModel

@Composable
internal fun SettingsContentV1(
    navController: NavController,
    vm: SettingsViewModel,
    state: SettingsUiState,
    connectionState: RingConnectionState,
    isFindingDevice: Boolean,
    openAiKey: String?,
    coachModel: String,
    useMaterialYou: Boolean,
    isCheckingUpdate: Boolean,
    bpCalSystolic: Int,
    bpCalDiastolic: Int,
    glucoseOffsetMgdl: Double,
    bloodMetricsEnabled: Boolean,
    onShowProfile: () -> Unit,
    onShowGoals: () -> Unit,
    onShowModel: () -> Unit,
    onShowForget: () -> Unit,
    onShowCalibration: () -> Unit
) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    val supportsBloodMetrics = state.capabilities.contains(WearableCapability.BLOOD_PRESSURE) ||
        state.capabilities.contains(WearableCapability.BLOOD_SUGAR)
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SectionHeader(stringResource(R.string.settings_section_profile))
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val profile = state.profile
                    StatusRow(stringResource(R.string.label_name), profile?.name ?: stringResource(R.string.value_not_set))
                    StatusRow(stringResource(R.string.label_age), profile?.age?.toString() ?: stringResource(R.string.value_not_set))
                    StatusRow(stringResource(R.string.label_sex), profile?.biologicalSex?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.value_not_set))
                    StatusRow(stringResource(R.string.label_height), profile?.heightCm?.let { "$it cm" } ?: stringResource(R.string.value_not_set))
                    StatusRow(stringResource(R.string.label_weight), profile?.weightKg?.let { "%.1f kg".format(it) } ?: stringResource(R.string.value_not_set))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(title = stringResource(R.string.action_edit_profile), onClick = onShowProfile)
        }

        item {
            SectionHeader(stringResource(R.string.settings_section_ring))
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val device = state.device
                    if (device != null) {
                        StatusRow(stringResource(R.string.label_device), device.name)
                        StatusRow(stringResource(R.string.label_address), device.macAddress)
                        StatusRow(stringResource(R.string.label_battery), "${device.batteryLevel ?: "--"}%")
                        StatusRow(stringResource(R.string.label_status), device.stateRaw.replaceFirstChar { it.uppercase() })
                        device.firmwareVersion?.let { StatusRow(stringResource(R.string.label_firmware), formatFirmwareVersion(it)) }
                    } else {
                        StatusRow("Status", stringResource(R.string.settings_ring_not_paired))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (state.device == null) {
                SecondaryButton(title = stringResource(R.string.action_pair_a_ring), onClick = { navController.navigate(NavRoute.Pairing.route) })
            } else {
                when (connectionState) {
                    RingConnectionState.CONNECTED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton(title = stringResource(R.string.action_sync_now), onClick = { vm.syncNow() }, modifier = Modifier.weight(1f))
                            SecondaryButton(title = if (isFindingDevice) stringResource(R.string.action_stop_finding) else stringResource(R.string.action_find_ring), onClick = { vm.toggleFindDevice() }, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SecondaryButton(title = stringResource(R.string.action_disconnect), onClick = { vm.disconnectDevice() })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    RingConnectionState.CONNECTING, RingConnectionState.RECONNECTING -> {
                        SecondaryButton(title = stringResource(R.string.vitals_connecting), onClick = {}, enabled = false)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    else -> {
                        SecondaryButton(title = stringResource(R.string.action_connect), onClick = { vm.reconnectDevice() })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                SecondaryButton(title = stringResource(R.string.action_forget_ring), onClick = onShowForget)
            }
        }

        item {
            SectionHeader(stringResource(R.string.settings_section_goals))
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow(stringResource(R.string.label_daily_steps), "%,d".format(state.goals?.dailySteps ?: 10_000))
                    StatusRow(stringResource(R.string.label_sleep), "${(state.goals?.sleepMinutes ?: 480) / 60}h ${(state.goals?.sleepMinutes ?: 480) % 60}m")
                    StatusRow(stringResource(R.string.label_active_minutes), "${state.goals?.activeMinutes ?: 45} min")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(title = stringResource(R.string.action_edit_goals), onClick = onShowGoals)
        }

        if (supportsBloodMetrics) {
            item {
                SectionHeader(stringResource(R.string.settings_section_blood_metrics))
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_blood_metrics_toggle), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(stringResource(R.string.settings_blood_metrics_subtitle), fontSize = 12.sp, color = colors.textSecondary)
                        }
                        Switch(checked = bloodMetricsEnabled, onCheckedChange = { vm.setBloodMetricsEnabled(it) })
                    }
                }
            }
            if (bloodMetricsEnabled) {
                item {
                    SectionHeader(stringResource(R.string.settings_section_calibration))
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.settings_calibration_subtitle), fontSize = 12.sp, color = colors.textSecondary)
                            StatusRow(
                                stringResource(R.string.settings_calibration_bp_label),
                                if (bpCalSystolic > 0 && bpCalDiastolic > 0)
                                    stringResource(R.string.settings_calibration_bp_value, bpCalSystolic, bpCalDiastolic)
                                else stringResource(R.string.settings_calibration_bp_unset)
                            )
                            StatusRow(
                                stringResource(R.string.settings_calibration_glucose_label),
                                if (glucoseOffsetMgdl != 0.0)
                                    stringResource(R.string.settings_calibration_glucose_value, "%+.0f".format(glucoseOffsetMgdl))
                                else stringResource(R.string.settings_calibration_glucose_unset)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(title = stringResource(R.string.settings_action_edit_calibration), onClick = onShowCalibration)
                }
            }
        } else if (state.device != null) {
            item {
                SectionHeader(stringResource(R.string.settings_section_calibration))
                PulseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_calibration_colmi_note), fontSize = 13.sp, color = colors.textSecondary)
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.settings_section_ai_coach))
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusRow(stringResource(R.string.label_openai), if (openAiKey.isNullOrBlank()) stringResource(R.string.value_not_connected) else stringResource(R.string.value_connected))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onShowModel() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.label_model), fontSize = 14.sp, color = colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(coachModel, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_model), tint = colors.textMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (openAiKey.isNullOrBlank()) {
                SecondaryButton(title = stringResource(R.string.action_login_with_openai), onClick = { vm.startOpenAIOAuth() })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(title = stringResource(R.string.action_re_login), onClick = { vm.startOpenAIOAuth() }, modifier = Modifier.weight(1f))
                    SecondaryButton(title = stringResource(R.string.action_logout), onClick = { vm.clearOpenAiAuth() }, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            PulseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_material_you_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(stringResource(R.string.settings_material_you_description), fontSize = 12.sp, color = colors.textSecondary)
                    }
                    Switch(checked = useMaterialYou, onCheckedChange = { vm.setUseMaterialYou(it) })
                }
            }
        }

        item {
            val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
            SectionHeader(stringResource(R.string.settings_section_about))
            PulseCard(modifier = Modifier.fillMaxWidth()) { StatusRow(stringResource(R.string.label_version), "${packageInfo.versionName}") }
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(title = stringResource(R.string.action_data_export), onClick = { navController.navigate(NavRoute.DataExport.route) })
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(title = if (isCheckingUpdate) stringResource(R.string.action_checking_for_updates) else stringResource(R.string.action_check_for_updates), enabled = !isCheckingUpdate, onClick = { vm.checkForUpdates() })
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalPulseColors.current
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textMuted,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun StatusRow(label: String, value: String) {
    val colors = LocalPulseColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = colors.textSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}
