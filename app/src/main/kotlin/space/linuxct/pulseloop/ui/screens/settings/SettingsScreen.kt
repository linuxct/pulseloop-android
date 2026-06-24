package space.linuxct.pulseloop.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.components.SecondaryButton
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.OAuthState
import space.linuxct.pulseloop.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val colors = LocalPulseColors.current
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val connectionState by vm.connectionState.collectAsState()
    val openAiKey by vm.openAiKey.collectAsState()
    val oauthState by vm.oauthState.collectAsState()
    val isCheckingUpdate by vm.isCheckingUpdate.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showForgetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Open browser when OAuth flow or update check emits a URL
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
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
                windowInsets = WindowInsets(0)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Profile section
                item {
                    SectionHeader("Profile")
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val profile = state.profile
                            StatusRow("Name", profile?.name ?: "Not set")
                            StatusRow("Age", profile?.age?.toString() ?: "Not set")
                            StatusRow("Sex", profile?.biologicalSex?.replaceFirstChar { it.uppercase() } ?: "Not set")
                            StatusRow("Height", profile?.heightCm?.let { "$it cm" } ?: "Not set")
                            StatusRow("Weight", profile?.weightKg?.let { "%.1f kg".format(it) } ?: "Not set")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(title = "Edit profile", onClick = { showProfileDialog = true })
                }

                // Ring section
                item {
                    SectionHeader("Ring")
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val device = state.device
                            if (device != null) {
                                StatusRow("Device", device.name)
                                StatusRow("Address", device.macAddress)
                                StatusRow("Battery", "${device.batteryLevel ?: "--"}%")
                                StatusRow("Status", device.stateRaw.replaceFirstChar { it.uppercase() })
                            } else {
                                StatusRow("Status", "No ring paired")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.device == null) {
                        SecondaryButton(title = "Pair a ring", onClick = {
                            navController.navigate(NavRoute.Pairing.route)
                        })
                    } else {
                        if (connectionState == RingConnectionState.CONNECTED) {
                            SecondaryButton(title = "Sync now", onClick = { vm.syncNow() })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SecondaryButton(title = "Forget ring", onClick = { showForgetDialog = true })
                    }
                }

                // Goals section
                item {
                    SectionHeader("Goals")
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusRow("Daily steps", "%,d".format(state.goals?.dailySteps ?: 10_000))
                            StatusRow("Sleep", "${(state.goals?.sleepMinutes ?: 480) / 60}h ${(state.goals?.sleepMinutes ?: 480) % 60}m")
                            StatusRow("Active minutes", "${state.goals?.activeMinutes ?: 45} min")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(title = "Edit goals", onClick = { showGoalDialog = true })
                }

                // AI Coach section
                item {
                    SectionHeader("AI Coach")
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusRow("OpenAI", if (openAiKey.isNullOrBlank()) "Not connected" else "Connected")
                            StatusRow("Model", "gpt-5.4")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (openAiKey.isNullOrBlank()) {
                        SecondaryButton(title = "Login with OpenAI", onClick = { vm.startOpenAIOAuth() })
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton(title = "Re-login", onClick = { vm.startOpenAIOAuth() }, modifier = Modifier.weight(1f))
                            SecondaryButton(title = "Logout", onClick = { vm.clearOpenAiAuth() }, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // About / version
                item {
                    val packageInfo = remember {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                    val versionString = "${packageInfo.versionName}"
                    SectionHeader("About")
                    PulseCard(modifier = Modifier.fillMaxWidth()) {
                        StatusRow("Version", versionString)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        title = if (isCheckingUpdate) "Checking…" else "Check for updates",
                        enabled = !isCheckingUpdate,
                        onClick = { vm.checkForUpdates() }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Profile edit dialog
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
                title = { Text("Edit Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Name") }, singleLine = true, colors = fieldColors)
                        OutlinedTextField(value = ageText, onValueChange = { ageText = it.filter { c -> c.isDigit() } }, label = { Text("Age") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Weight (kg)") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = heightText, onValueChange = { heightText = it.filter { c -> c.isDigit() } }, label = { Text("Height (cm)") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        Text("Sex", fontSize = 13.sp, color = colors.textSecondary)
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
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") } },
                containerColor = colors.card
            )
        }

        // Goals edit dialog
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
                title = { Text("Edit Goals") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stepsText, onValueChange = { stepsText = it.filter { c -> c.isDigit() } }, label = { Text("Daily steps") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = sleepMinsText, onValueChange = { sleepMinsText = it.filter { c -> c.isDigit() } }, label = { Text("Sleep (minutes)") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = activeMinsText, onValueChange = { activeMinsText = it.filter { c -> c.isDigit() } }, label = { Text("Active minutes") }, singleLine = true, colors = fieldColors, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.updateGoals(
                            steps = stepsText.toIntOrNull() ?: 10_000,
                            sleepMinutes = sleepMinsText.toIntOrNull() ?: 480,
                            activeMinutes = activeMinsText.toIntOrNull() ?: 45
                        )
                        showGoalDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } },
                containerColor = colors.card
            )
        }

        // Forget ring confirmation
        if (showForgetDialog) {
            AlertDialog(
                onDismissRequest = { showForgetDialog = false },
                title = { Text("Forget ring?") },
                text = { Text("This will unpair your ring. You can pair it again at any time.", color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { vm.forgetDevice(); showForgetDialog = false }) {
                        Text("Forget", color = colors.danger)
                    }
                },
                dismissButton = { TextButton(onClick = { showForgetDialog = false }) { Text("Cancel") } },
                containerColor = colors.card
            )
        }

        // OAuth waiting dialog
        if (oauthState is OAuthState.Waiting) {
            AlertDialog(
                onDismissRequest = { vm.cancelOAuth() },
                title = { Text("Waiting for OpenAI") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                        Text(
                            "Complete the login in your browser, then return here.",
                            fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { vm.cancelOAuth() }) { Text("Cancel") }
                },
                containerColor = colors.card
            )
        }

        // OAuth success dialog
        if (oauthState is OAuthState.Success) {
            AlertDialog(
                onDismissRequest = { vm.dismissOAuthResult() },
                title = { Text("Connected") },
                text = { Text("You're now logged in to OpenAI. AI Coach is ready.", color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { vm.dismissOAuthResult() }) { Text("OK") }
                },
                containerColor = colors.card
            )
        }

        // OAuth error dialog
        if (oauthState is OAuthState.Error) {
            val err = (oauthState as OAuthState.Error).message
            AlertDialog(
                onDismissRequest = { vm.dismissOAuthResult() },
                title = { Text("Login failed") },
                text = { Text(err, fontSize = 14.sp, color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { vm.dismissOAuthResult() }) { Text("OK") }
                },
                containerColor = colors.card
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.card,
                    contentColor = colors.textPrimary
                )
            }
        )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = colors.textSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}
