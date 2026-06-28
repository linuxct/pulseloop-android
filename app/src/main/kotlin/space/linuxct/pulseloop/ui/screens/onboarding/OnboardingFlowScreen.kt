package space.linuxct.pulseloop.ui.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.components.PrimaryButton
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.ShellViewModel

@Composable
fun OnboardingFlowScreen(
    onFinished: () -> Unit,
    onPairNow: () -> Unit = onFinished,
    vm: ShellViewModel = hiltViewModel()
) {
    val colors = LocalPulseColors.current
    val steps = listOf("Welcome", "Profile", "Baseline", "Goals", "Pair")
    var step by remember { mutableIntStateOf(0) }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onPairNow()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEachIndexed { i, _ ->
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i <= step) colors.accent else colors.cardSoft,
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                0 -> OnboardingPage(
                    title = "PulseLoop",
                    subtitle = stringResource(R.string.onboarding_welcome_subtitle),
                    actionTitle = stringResource(R.string.onboarding_action_get_started)
                ) { step++ }
                1 -> ProfilePage { name, age, sex, weight, height ->
                    vm.saveProfile(name, age, sex, weight, height)
                    step++
                }
                2 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_baseline_title),
                    subtitle = stringResource(R.string.onboarding_baseline_subtitle),
                    actionTitle = stringResource(R.string.action_continue)
                ) { step++ }
                3 -> GoalsPage { stepsGoal, sleepMinutes, activeMinutes ->
                    vm.saveGoals(stepsGoal, sleepMinutes, activeMinutes)
                    step++
                }
                4 -> PairPage(
                    onPairNow = {
                        blePermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                            )
                        )
                    },
                    onSkip = { onFinished() }
                )
            }
        }
    }
}

@Composable
private fun ProfilePage(onContinue: (name: String?, age: Int?, sex: String?, weightKg: Double?, heightCm: Int?) -> Unit) {
    val colors = LocalPulseColors.current
    var nameText by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderStrong,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textMuted,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.accent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.onboarding_profile_title), fontSize = 34.sp, color = colors.textPrimary)
        Text(
            stringResource(R.string.onboarding_profile_description),
            fontSize = 15.sp,
            color = colors.textSecondary
        )

        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it.take(50) },
            label = { Text(stringResource(R.string.onboarding_label_your_name)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text(stringResource(R.string.label_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.label_biological_sex), fontSize = 13.sp, color = colors.textMuted)
            val maleLabel = stringResource(R.string.label_sex_male)
            val femaleLabel = stringResource(R.string.label_sex_female)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(maleLabel to "male", femaleLabel to "female").forEach { (label, value) ->
                    FilterChip(
                        selected = sex == value,
                        onClick = { sex = if (sex == value) "" else value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            selectedLabelColor = colors.accent,
                            labelColor = colors.textSecondary,
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
            label = { Text(stringResource(R.string.label_weight_kg)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text(stringResource(R.string.label_height_cm)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            title = stringResource(R.string.action_continue),
            onClick = {
                onContinue(
                    nameText.ifBlank { null },
                    ageText.toIntOrNull(),
                    sex.ifBlank { null },
                    weightText.toDoubleOrNull(),
                    heightText.toIntOrNull()
                )
            }
        )
        SecondaryButton(
            title = stringResource(R.string.action_skip_for_now),
            onClick = { onContinue(null, null, null, null, null) }
        )
    }
}

@Composable
private fun GoalsPage(onContinue: (steps: Int, sleepMinutes: Int, activeMinutes: Int) -> Unit) {
    val colors = LocalPulseColors.current
    var stepsText by remember { mutableStateOf("10000") }
    var sleepHoursText by remember { mutableStateOf("8") }
    var activeMinsText by remember { mutableStateOf("45") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.borderStrong,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textMuted,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.accent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.onboarding_goals_title), fontSize = 34.sp, color = colors.textPrimary)
        Text(
            stringResource(R.string.onboarding_goals_description),
            fontSize = 15.sp,
            color = colors.textSecondary
        )

        OutlinedTextField(
            value = stepsText,
            onValueChange = { stepsText = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text(stringResource(R.string.label_daily_steps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        OutlinedTextField(
            value = sleepHoursText,
            onValueChange = { sleepHoursText = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
            label = { Text(stringResource(R.string.label_sleep_goal_hours)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        OutlinedTextField(
            value = activeMinsText,
            onValueChange = { activeMinsText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text(stringResource(R.string.label_active_minutes)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            title = stringResource(R.string.action_continue),
            onClick = {
                onContinue(
                    stepsText.toIntOrNull()?.coerceIn(1_000, 100_000) ?: 10_000,
                    ((sleepHoursText.toFloatOrNull() ?: 8f) * 60).toInt().coerceIn(60, 720),
                    activeMinsText.toIntOrNull()?.coerceIn(5, 300) ?: 45
                )
            }
        )
    }
}

@Composable
private fun PairPage(
    onPairNow: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = LocalPulseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_pair_title),
            fontSize = 34.sp,
            color = colors.textPrimary
        )
        Text(
            text = stringResource(R.string.onboarding_pair_subtitle),
            fontSize = 15.sp,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(title = stringResource(R.string.action_pair_now), onClick = onPairNow)
        SecondaryButton(title = stringResource(R.string.action_skip_for_now), onClick = onSkip)
    }
}

@Composable
private fun OnboardingPage(
    title: String,
    subtitle: String,
    actionTitle: String,
    onAction: () -> Unit
) {
    val colors = LocalPulseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 34.sp,
            color = colors.textPrimary
        )
        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = colors.textSecondary
        )
        PrimaryButton(title = actionTitle, onClick = onAction)
    }
}
