package space.linuxct.pulseloop.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.screens.settings.v1.SettingsContentV1
import space.linuxct.pulseloop.ui.screens.settings.v2.SettingsContentV2
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.theme.LocalUiMode
import space.linuxct.pulseloop.ui.theme.UiMode
import space.linuxct.pulseloop.ui.viewmodel.OAuthState
import space.linuxct.pulseloop.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val connectionState by vm.connectionState.collectAsState()
    val isFindingDevice by vm.isFindingDevice.collectAsState()
    val openAiKey by vm.openAiKey.collectAsState()
    val oauthState by vm.oauthState.collectAsState()
    val isCheckingUpdate by vm.isCheckingUpdate.collectAsState()
    val coachModel by vm.coachModel.collectAsState()
    val useMaterialYou by vm.useMaterialYou.collectAsState()
    val bpCalSystolic by vm.bpCalSystolic.collectAsState()
    val bpCalDiastolic by vm.bpCalDiastolic.collectAsState()
    val glucoseOffsetMgdl by vm.glucoseOffsetMgdl.collectAsState()
    val glucoseRefMgdl by vm.glucoseRefMgdl.collectAsState()
    val bloodMetricsEnabled by vm.bloodMetricsEnabled.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showForgetDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isMY = LocalUiMode.current == UiMode.MATERIAL_YOU

    LaunchedEffect(Unit) {
        vm.launchBrowser.collect { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    LaunchedEffect(Unit) {
        vm.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isMY) MaterialTheme.colorScheme.background else colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMY) {
                SettingsLargeHeaderV2(
                    title = stringResource(R.string.screen_title_settings),
                    onBack = { navController.popBackStack() }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.screen_title_settings), color = colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                    windowInsets = WindowInsets(0)
                )
            }

            if (isMY) {
                SettingsContentV2(
                    navController = navController,
                    vm = vm,
                    state = state,
                    connectionState = connectionState,
                    isFindingDevice = isFindingDevice,
                    openAiKey = openAiKey,
                    coachModel = coachModel,
                    useMaterialYou = useMaterialYou,
                    isCheckingUpdate = isCheckingUpdate,
                    bpCalSystolic = bpCalSystolic,
                    bpCalDiastolic = bpCalDiastolic,
                    glucoseOffsetMgdl = glucoseOffsetMgdl,
                    bloodMetricsEnabled = bloodMetricsEnabled,
                    onShowProfile = { showProfileDialog = true },
                    onShowGoals = { showGoalDialog = true },
                    onShowModel = { showModelDialog = true },
                    onShowForget = { showForgetDialog = true },
                    onShowCalibration = { showCalibrationDialog = true }
                )
            } else {
                SettingsContentV1(
                    navController = navController,
                    vm = vm,
                    state = state,
                    connectionState = connectionState,
                    isFindingDevice = isFindingDevice,
                    openAiKey = openAiKey,
                    coachModel = coachModel,
                    useMaterialYou = useMaterialYou,
                    isCheckingUpdate = isCheckingUpdate,
                    bpCalSystolic = bpCalSystolic,
                    bpCalDiastolic = bpCalDiastolic,
                    glucoseOffsetMgdl = glucoseOffsetMgdl,
                    bloodMetricsEnabled = bloodMetricsEnabled,
                    onShowProfile = { showProfileDialog = true },
                    onShowGoals = { showGoalDialog = true },
                    onShowModel = { showModelDialog = true },
                    onShowForget = { showForgetDialog = true },
                    onShowCalibration = { showCalibrationDialog = true }
                )
            }

        }

        // ── Shared dialogs ────────────────────────────────────────────────

        if (showModelDialog) {
            var modelText by remember { mutableStateOf(coachModel) }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderStrong,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textMuted,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accent
            )
            AlertDialog(
                onDismissRequest = { showModelDialog = false },
                title = { Text(stringResource(R.string.dialog_ai_model_title)) },
                text = {
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        label = { Text(stringResource(R.string.dialog_ai_model_field_label)) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val trimmed = modelText.trim()
                        if (trimmed.isNotBlank()) vm.setCoachModel(trimmed)
                        showModelDialog = false
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = { TextButton(onClick = { showModelDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (showProfileDialog) {
            val profile = state.profile
            var nameText by remember { mutableStateOf(profile?.name ?: "") }
            var ageText by remember { mutableStateOf(profile?.age?.toString() ?: "") }
            var sexText by remember { mutableStateOf(profile?.biologicalSex ?: "") }
            var weightText by remember { mutableStateOf(profile?.weightKg?.let { "%.1f".format(it) } ?: "") }
            var heightText by remember { mutableStateOf(profile?.heightCm?.toString() ?: "") }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderStrong,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textMuted,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accent
            )
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = { Text(stringResource(R.string.dialog_edit_profile_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text(stringResource(R.string.label_name)) }, singleLine = true, colors = fieldColors)
                        OutlinedTextField(value = ageText, onValueChange = { ageText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.label_age)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text(stringResource(R.string.label_weight_kg)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = heightText, onValueChange = { heightText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.label_height_cm)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text(stringResource(R.string.label_sex), fontSize = 13.sp, color = colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("male", "female", "other").forEach { option ->
                                FilterChip(
                                    selected = sexText == option,
                                    onClick = { sexText = option },
                                    label = { Text(option.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = colors.accent
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateProfile(
                            name = nameText.trim().takeIf { it.isNotBlank() },
                            age = ageText.toIntOrNull(),
                            biologicalSex = sexText.takeIf { it.isNotBlank() },
                            weightKg = weightText.toDoubleOrNull(),
                            heightCm = heightText.toIntOrNull()
                        )
                        showProfileDialog = false
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (showGoalDialog) {
            var stepsText by remember { mutableStateOf(state.goals?.dailySteps?.toString() ?: "10000") }
            var sleepMinsText by remember { mutableStateOf(state.goals?.sleepMinutes?.toString() ?: "480") }
            var activeMinsText by remember { mutableStateOf(state.goals?.activeMinutes?.toString() ?: "45") }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderStrong,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textMuted,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accent
            )
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { Text(stringResource(R.string.dialog_edit_goals_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stepsText, onValueChange = { stepsText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.label_daily_steps)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = sleepMinsText, onValueChange = { sleepMinsText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.label_sleep_minutes)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = activeMinsText, onValueChange = { activeMinsText = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.label_active_minutes)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateGoals(steps = stepsText.toIntOrNull() ?: 10_000, sleepMinutes = sleepMinsText.toIntOrNull() ?: 480, activeMinutes = activeMinsText.toIntOrNull() ?: 45)
                        showGoalDialog = false
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (showCalibrationDialog) {
            var sysText by remember { mutableStateOf(bpCalSystolic.takeIf { it > 0 }?.toString() ?: "") }
            var diaText by remember { mutableStateOf(bpCalDiastolic.takeIf { it > 0 }?.toString() ?: "") }
            var glucoseRefText by remember { mutableStateOf(glucoseRefMgdl.takeIf { it > 0.0 }?.let { "%.0f".format(it) } ?: "") }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderStrong,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textMuted,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accent
            )
            AlertDialog(
                onDismissRequest = { showCalibrationDialog = false },
                title = { Text(stringResource(R.string.dialog_calibration_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.dialog_calibration_bp_header), fontSize = 13.sp, color = colors.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = sysText, onValueChange = { sysText = it.filter { c -> c.isDigit() }.take(3) }, label = { Text(stringResource(R.string.dialog_calibration_bp_systolic)) }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = diaText, onValueChange = { diaText = it.filter { c -> c.isDigit() }.take(3) }, label = { Text(stringResource(R.string.dialog_calibration_bp_diastolic)) }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Text(stringResource(R.string.dialog_calibration_bp_help), fontSize = 12.sp, color = colors.textMuted, lineHeight = 16.sp)
                        Text(stringResource(R.string.dialog_calibration_glucose_header), fontSize = 13.sp, color = colors.textSecondary)
                        OutlinedTextField(value = glucoseRefText, onValueChange = { glucoseRefText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.dialog_calibration_glucose_field)) }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Text(stringResource(R.string.dialog_calibration_glucose_help), fontSize = 12.sp, color = colors.textMuted, lineHeight = 16.sp)
                        TextButton(onClick = { vm.resetGlucoseCalibration(); glucoseRefText = "" }) {
                            Text(stringResource(R.string.dialog_calibration_glucose_reset), color = colors.danger)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setBpCalibration(sysText.toIntOrNull() ?: 0, diaText.toIntOrNull() ?: 0)
                        glucoseRefText.toDoubleOrNull()?.let { if (it > 0.0) vm.calibrateGlucose(it) }
                        showCalibrationDialog = false
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = { TextButton(onClick = { showCalibrationDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (showForgetDialog) {
            AlertDialog(
                onDismissRequest = { showForgetDialog = false },
                title = { Text(stringResource(R.string.dialog_forget_ring_title)) },
                text = { Text(stringResource(R.string.dialog_forget_ring_message), color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { vm.forgetDevice(); showForgetDialog = false }) {
                        Text(stringResource(R.string.action_forget), color = colors.danger)
                    }
                },
                dismissButton = { TextButton(onClick = { showForgetDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (oauthState is OAuthState.Waiting) {
            AlertDialog(
                onDismissRequest = { vm.cancelOAuth() },
                title = { Text(stringResource(R.string.dialog_oauth_waiting_title)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                        Text(stringResource(R.string.dialog_oauth_waiting_message), fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { vm.cancelOAuth() }) { Text(stringResource(R.string.action_cancel)) } },
                containerColor = colors.card
            )
        }

        if (oauthState is OAuthState.Success) {
            AlertDialog(
                onDismissRequest = { vm.dismissOAuthResult() },
                title = { Text(stringResource(R.string.dialog_oauth_success_title)) },
                text = { Text(stringResource(R.string.dialog_oauth_success_message), color = colors.textSecondary) },
                confirmButton = { TextButton(onClick = { vm.dismissOAuthResult() }) { Text(stringResource(R.string.action_ok)) } },
                containerColor = colors.card
            )
        }

        if (oauthState is OAuthState.Error) {
            val err = (oauthState as OAuthState.Error).message
            AlertDialog(
                onDismissRequest = { vm.dismissOAuthResult() },
                title = { Text(stringResource(R.string.dialog_oauth_error_title)) },
                text = { Text(err, fontSize = 14.sp, color = colors.textSecondary) },
                confirmButton = { TextButton(onClick = { vm.dismissOAuthResult() }) { Text(stringResource(R.string.action_ok)) } },
                containerColor = colors.card
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            snackbar = { data ->
                Snackbar(snackbarData = data, containerColor = colors.card, contentColor = colors.textPrimary)
            }
        )
    }
}

@Composable
private fun SettingsLargeHeaderV2(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        FilledTonalIconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp)
        )
    }
}

/**
 * The ring reports firmware as the official "<CID><DID>V<version>" string (e.g. "003A002AV138").
 * Settings only needs the human-readable version segment, so show everything from "V" onward.
 */
fun formatFirmwareVersion(raw: String): String {
    val version = raw.substringAfterLast('V', "")
    return if (version.isNotEmpty()) "V$version" else raw
}
