package space.linuxct.pulseloop.ui.screens.settings.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.screens.settings.formatFirmwareVersion
import space.linuxct.pulseloop.ui.viewmodel.SettingsUiState
import space.linuxct.pulseloop.ui.viewmodel.SettingsViewModel

// ── Grouped-list visual constants (Android 16 / MD3 Expressive settings) ───────
private val GroupOuterCorner = 20.dp
private val GroupInnerCorner = 4.dp
private val RowGap = 2.dp
private val GroupGap = 8.dp

@Composable
internal fun SettingsContentV2(
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
    val context = LocalContext.current
    val notSet = stringResource(R.string.value_not_set)
    val supportsBloodMetrics = state.capabilities.contains(WearableCapability.BLOOD_PRESSURE) ||
        state.capabilities.contains(WearableCapability.BLOOD_SUGAR)

    // A plain scrollable Column (not LazyColumn): settings is short, and eager composition means
    // every row recomposes when async state arrives (profile from the DB, the opt-in from DataStore).
    // The LazyColumn left already-composed items stale until they were scrolled off-screen and back.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp)
    ) {
        // ── Profile ───────────────────────────────────────────────────────────
        val profile = state.profile
        SettingsSection(stringResource(R.string.settings_section_profile)) {
            SettingsGroup {
                value(stringResource(R.string.label_name), profile?.name ?: notSet)
                value(stringResource(R.string.label_age), profile?.age?.toString() ?: notSet)
                value(stringResource(R.string.label_sex), profile?.biologicalSex?.replaceFirstChar { it.uppercase() } ?: notSet)
                value(stringResource(R.string.label_height), profile?.heightCm?.let { "$it cm" } ?: notSet)
                value(stringResource(R.string.label_weight), profile?.weightKg?.let { "%.1f kg".format(it) } ?: notSet)
                nav(stringResource(R.string.action_edit_profile), onClick = onShowProfile)
            }
        }

        // ── Ring ──────────────────────────────────────────────────────────────
        val device = state.device
        SettingsSection(stringResource(R.string.settings_section_ring)) {
            if (device != null) {
                SettingsGroup {
                    value(stringResource(R.string.label_device), device.name)
                    value(stringResource(R.string.label_battery), "${device.batteryLevel ?: "--"}%")
                    value(stringResource(R.string.label_status), device.stateRaw.replaceFirstChar { it.uppercase() })
                    device.firmwareVersion?.let { value(stringResource(R.string.label_firmware), formatFirmwareVersion(it)) }
                    when (connectionState) {
                        RingConnectionState.CONNECTED -> {
                            action(stringResource(R.string.action_sync_now)) { vm.syncNow() }
                            action(if (isFindingDevice) stringResource(R.string.action_stop_finding) else stringResource(R.string.action_find_ring)) { vm.toggleFindDevice() }
                            action(stringResource(R.string.action_disconnect)) { vm.disconnectDevice() }
                        }
                        RingConnectionState.CONNECTING, RingConnectionState.RECONNECTING ->
                            info(stringResource(R.string.vitals_connecting))
                        else -> action(stringResource(R.string.action_connect)) { vm.reconnectDevice() }
                    }
                    action(stringResource(R.string.action_forget_ring), tone = ActionTone.ERROR) { onShowForget() }
                }
            } else {
                SettingsGroup {
                    info(stringResource(R.string.settings_ring_not_paired))
                    action(stringResource(R.string.action_pair_a_ring)) { navController.navigate(NavRoute.Pairing.route) }
                }
            }
        }

        // ── Goals ─────────────────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_goals)) {
            SettingsGroup {
                value(stringResource(R.string.label_daily_steps), "%,d".format(state.goals?.dailySteps ?: 10_000))
                value(stringResource(R.string.label_sleep), "${(state.goals?.sleepMinutes ?: 480) / 60}h ${(state.goals?.sleepMinutes ?: 480) % 60}m")
                value(stringResource(R.string.label_active_minutes), "${state.goals?.activeMinutes ?: 45} min")
                nav(stringResource(R.string.action_edit_goals), onClick = onShowGoals)
            }
        }

        // ── Estimated blood metrics opt-in + calibration (Jring only) ──────────
        if (supportsBloodMetrics) {
            SettingsSection(stringResource(R.string.settings_section_blood_metrics)) {
                SettingsGroup {
                    switch(
                        label = stringResource(R.string.settings_blood_metrics_toggle),
                        subtitle = stringResource(R.string.settings_blood_metrics_subtitle),
                        checked = bloodMetricsEnabled,
                        onChange = { vm.setBloodMetricsEnabled(it) }
                    )
                }
                if (bloodMetricsEnabled) {
                    SettingsGroup {
                        value(
                            stringResource(R.string.settings_calibration_bp_label),
                            if (bpCalSystolic > 0 && bpCalDiastolic > 0)
                                stringResource(R.string.settings_calibration_bp_value, bpCalSystolic, bpCalDiastolic)
                            else stringResource(R.string.settings_calibration_bp_unset)
                        )
                        value(
                            stringResource(R.string.settings_calibration_glucose_label),
                            if (glucoseOffsetMgdl != 0.0)
                                stringResource(R.string.settings_calibration_glucose_value, "%+.0f".format(glucoseOffsetMgdl))
                            else stringResource(R.string.settings_calibration_glucose_unset)
                        )
                        nav(stringResource(R.string.settings_action_edit_calibration), onClick = onShowCalibration)
                    }
                }
            }
        } else if (state.device != null) {
            SettingsSection(stringResource(R.string.settings_section_calibration)) {
                SettingsGroup {
                    info(stringResource(R.string.settings_calibration_colmi_note))
                }
            }
        }

        // ── AI Coach ──────────────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_ai_coach)) {
            SettingsGroup {
                value(stringResource(R.string.label_openai), if (openAiKey.isNullOrBlank()) stringResource(R.string.value_not_connected) else stringResource(R.string.value_connected))
                nav(stringResource(R.string.label_model), value = coachModel, trailing = NavTrailing.EDIT, onClick = onShowModel)
                if (openAiKey.isNullOrBlank()) {
                    action(stringResource(R.string.action_login_with_openai)) { vm.startOpenAIOAuth() }
                } else {
                    action(stringResource(R.string.action_re_login)) { vm.startOpenAIOAuth() }
                    action(stringResource(R.string.action_logout)) { vm.clearOpenAiAuth() }
                }
            }
        }

        // ── Appearance ────────────────────────────────────────────────────────
        SettingsSection(stringResource(R.string.settings_section_appearance)) {
            SettingsGroup {
                switch(
                    label = stringResource(R.string.settings_material_you_label),
                    subtitle = stringResource(R.string.settings_material_you_description),
                    checked = useMaterialYou,
                    onChange = { vm.setUseMaterialYou(it) }
                )
            }
        }

        // ── About ─────────────────────────────────────────────────────────────
        val packageInfo = androidx.compose.runtime.remember { context.packageManager.getPackageInfo(context.packageName, 0) }
        SettingsSection(stringResource(R.string.settings_section_about)) {
            SettingsGroup {
                value(stringResource(R.string.label_version), "${packageInfo.versionName}")
                nav(stringResource(R.string.action_data_export)) { navController.navigate(NavRoute.DataExport.route) }
                action(
                    if (isCheckingUpdate) stringResource(R.string.action_checking_for_updates) else stringResource(R.string.action_check_for_updates),
                    enabled = !isCheckingUpdate
                ) { vm.checkForUpdates() }
                nav(stringResource(R.string.action_debug_menu)) { navController.navigate(NavRoute.Debug.route) }
            }
        }
    }
}

// ── Section + group scaffolding ────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 10.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(GroupGap), content = content)
}

@Composable
private fun SettingsGroup(content: @Composable GroupScope.() -> Unit) {
    // Each row is rendered DIRECTLY by a @Composable builder method (below) instead of being
    // collected into a List<RowSpec> and rendered separately. The list approach did not recompose
    // when async state arrived (profile from the DB, calibration, the opt-in from DataStore) — rows
    // stayed on their initial values until the list happened to be scroll-recycled. Direct
    // composition is exactly what the V1 settings screen does, which always refreshed correctly.
    // The group clips to rounded outer corners; rows are surfaceContainerHigh with hairline gaps.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupOuterCorner)),
        verticalArrangement = Arrangement.spacedBy(RowGap)
    ) {
        val scope = GroupScope()
        scope.content()
    }
}

// ── Row rendering ──────────────────────────────────────────────────────────────

@Composable
private fun SettingsRowContainer(onClick: (() -> Unit)?, content: @Composable RowScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var rowModifier = Modifier
        .fillMaxWidth()
        .background(scheme.surfaceContainerHigh)
    if (onClick != null) rowModifier = rowModifier.clickable(onClick = onClick)
    Row(
        modifier = rowModifier
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

private enum class ActionTone { PRIMARY, ERROR }
private enum class NavTrailing { CHEVRON, EDIT }

/** Builder whose methods are @Composable and emit their row immediately (no intermediate list). */
private class GroupScope {
    @Composable
    fun value(label: String, value: String) {
        val scheme = MaterialTheme.colorScheme
        SettingsRowContainer(onClick = null) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
    }

    @Composable
    fun info(text: String) {
        val scheme = MaterialTheme.colorScheme
        SettingsRowContainer(onClick = null) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    fun action(label: String, tone: ActionTone = ActionTone.PRIMARY, enabled: Boolean = true, onClick: () -> Unit) {
        val scheme = MaterialTheme.colorScheme
        SettingsRowContainer(onClick = if (enabled) onClick else null) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = when {
                    !enabled -> scheme.onSurfaceVariant
                    tone == ActionTone.ERROR -> scheme.error
                    else -> scheme.primary
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    fun nav(label: String, subtitle: String? = null, value: String? = null, trailing: NavTrailing = NavTrailing.CHEVRON, onClick: () -> Unit) {
        val scheme = MaterialTheme.colorScheme
        SettingsRowContainer(onClick = onClick) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            if (value != null) {
                Spacer(Modifier.width(12.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.End)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (trailing == NavTrailing.EDIT) Icons.Filled.Edit else Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(if (trailing == NavTrailing.EDIT) 18.dp else 24.dp)
            )
        }
    }

    @Composable
    fun switch(label: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
        val scheme = MaterialTheme.colorScheme
        SettingsRowContainer(onClick = { onChange(!checked) }) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
