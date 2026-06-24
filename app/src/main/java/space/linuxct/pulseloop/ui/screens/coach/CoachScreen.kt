package space.linuxct.pulseloop.ui.screens.coach

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.coach.model.CoachResponse
import space.linuxct.pulseloop.ui.charts.CoachChartCard
import space.linuxct.pulseloop.ui.components.PulseCard
import space.linuxct.pulseloop.ui.navigation.LocalBottomNavHeight
import space.linuxct.pulseloop.ui.navigation.NavRoute
import space.linuxct.pulseloop.ui.theme.LocalPulseColors
import space.linuxct.pulseloop.ui.viewmodel.CoachMessageUi
import space.linuxct.pulseloop.ui.viewmodel.CoachViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(navController: NavController, vm: CoachViewModel = hiltViewModel()) {
    val colors  = LocalPulseColors.current
    val state   by vm.uiState.collectAsState()
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbar  = remember { SnackbarHostState() }
    val bottomNavHeight = LocalBottomNavHeight.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomNavPx = with(density) { bottomNavHeight.roundToPx() }
    val contentInsets = WindowInsets.ime.union(WindowInsets(0, 0, 0, bottomNavPx))

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size, state.isLoading) {
        when {
            state.isLoading ->
                // Keep the loading/streaming bubble visible (it sits at messages.size)
                listState.animateScrollToItem(maxOf(0, state.messages.size))
            state.messages.isNotEmpty() ->
                listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            scope.launch { snackbar.showSnackbar(msg) }
            vm.clearError()
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || state.isLoading) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.send(text)
        inputText = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(contentInsets)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(listOf(colors.accent, colors.spo2, colors.accent))
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Coach", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.newConversation()
                }) {
                    Icon(Icons.Default.AddComment, contentDescription = "New conversation", tint = colors.textSecondary)
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showClearDialog = true
                    },
                    enabled = state.messages.isNotEmpty() && !state.isLoading
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear chat history",
                        tint = if (state.messages.isNotEmpty() && !state.isLoading) Color(0xFFE53935) else colors.textMuted
                    )
                }
            }

            HorizontalDivider(color = colors.borderSubtle)

            // ── No API key gate ───────────────────────────────────────────
            if (!state.hasApiKey) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseCard(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("AI Coach", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                            Text(
                                "Log in with OpenAI in Settings → AI Coach to start chatting with your personal health coach.",
                                fontSize = 14.sp, color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                lineHeight = 20.sp
                            )
                            androidx.compose.material3.Button(
                                onClick = { navController.navigate(NavRoute.Settings.route) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("Open Settings", color = Color.White)
                            }
                        }
                    }
                }
                return@Column
            }

            // ── Message list ──────────────────────────────────────────────
            val lastIdx = state.messages.lastIndex
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    item { WelcomeCard(onChipTap = { inputText = it }) }
                }

                items(state.messages, key = { it.id }) { msg ->
                    val isLast = state.messages.indexOf(msg) == lastIdx
                    MessageBubble(
                        msg = msg,
                        showChips = isLast && msg.role == "assistant" && !state.isLoading,
                        onChipTap = { chip ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.send(chip)
                        }
                    )
                }

                if (state.isLoading && state.streamingText == null) {
                    item { LoadingBubble(traceEvents = state.traceEvents) }
                }
                state.streamingText?.let { raw ->
                    item(key = "streaming") { StreamingBubble(rawJson = raw) }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────
            HorizontalDivider(color = colors.borderSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask your coach…", color = colors.textMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colors.accent,
                        unfocusedBorderColor = colors.borderStrong,
                        focusedTextColor     = colors.textPrimary,
                        unfocusedTextColor   = colors.textPrimary,
                        cursorColor          = colors.accent
                    )
                )
                IconButton(
                    onClick = { sendMessage() },
                    enabled = inputText.isNotBlank() && !state.isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !state.isLoading) colors.accent else colors.textMuted
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomNavHeight + 80.dp)
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear chat history") },
            text = { Text("This will permanently delete all messages in this conversation. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.clearHistory()
                }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(
    msg: CoachMessageUi,
    showChips: Boolean,
    onChipTap: (String) -> Unit
) {
    val colors = LocalPulseColors.current

    if (msg.role == "user") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                    .background(colors.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(msg.text, fontSize = 14.sp, color = colors.textPrimary, lineHeight = 20.sp)
            }
        }
        return
    }

    // Assistant message
    val response = msg.response
    Column(modifier = Modifier.fillMaxWidth()) {
        PulseCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (response != null) {
                    // Title
                    if (response.title.isNotBlank()) {
                        Text(response.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                    }
                    // Summary
                    if (response.summary.isNotBlank()) {
                        Text(response.summary, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                    }
                    // Bullets
                    if (response.bullets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            response.bullets.forEach { bullet ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("•", fontSize = 14.sp, color = colors.accent)
                                    Text(bullet, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                    // Chart (if provided)
                    response.chart?.let { chart ->
                        Spacer(modifier = Modifier.height(4.dp))
                        CoachChartCard(chart = chart, modifier = Modifier.fillMaxWidth())
                    }
                    // Data quality note
                    response.dataQualityNote?.let { note ->
                        Text(note, fontSize = 12.sp, color = colors.textMuted, lineHeight = 16.sp)
                    }
                    // Safety note
                    response.safetyNote?.let { note ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.danger.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = colors.danger, modifier = Modifier.size(14.dp))
                            Text(note, fontSize = 12.sp, color = colors.danger, lineHeight = 16.sp)
                        }
                    }
                    // Confidence chip
                    ConfidencePill(response.confidence)
                } else {
                    Text(msg.text, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                }
            }
        }

        // Follow-up chips (only on last assistant message)
        if (showChips && response != null && response.followUpChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                response.followUpChips.forEach { chip ->
                    SuggestionChip(
                        onClick = { onChipTap(chip) },
                        label = { Text(chip, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = colors.elevated,
                            labelColor = colors.accent
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = colors.accent.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

// ── Loading bubble ────────────────────────────────────────────────────────────

@Composable
private fun LoadingBubble(traceEvents: List<String>) {
    val colors = LocalPulseColors.current
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp).alpha(alpha),
                color = colors.accent, strokeWidth = 2.dp
            )
            Text(
                traceEvents.lastOrNull() ?: "Thinking…",
                fontSize = 13.sp, color = colors.textMuted,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

// ── Streaming bubble ──────────────────────────────────────────────────────────
// Shows while the model streams its JSON response. Incrementally extracts and displays
// the human-readable title and summary fields from the partial JSON as they arrive.

@Composable
private fun StreamingBubble(rawJson: String) {
    val colors  = LocalPulseColors.current
    val title   = remember(rawJson) { extractJsonField(rawJson, "title") }
    val summary = remember(rawJson) { extractJsonField(rawJson, "summary") }

    val cursorTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink"
    )

    PulseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                summary.isNotBlank() -> {
                    if (title.isNotBlank()) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                    }
                    val displayText = buildAnnotatedString {
                        append(summary)
                        withStyle(SpanStyle(color = colors.accent.copy(alpha = cursorAlpha))) { append("▌") }
                    }
                    Text(displayText, fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
                }
                title.isNotBlank() -> {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = colors.accent, strokeWidth = 1.5.dp)
                        Text("Composing…", fontSize = 13.sp, color = colors.textMuted)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = colors.accent, strokeWidth = 1.5.dp)
                        Text("Generating response…", fontSize = 13.sp, color = colors.textMuted)
                    }
                }
            }
        }
    }
}

/** Extracts a JSON string-field value from a potentially incomplete (mid-stream) JSON blob. */
private fun extractJsonField(json: String, field: String): String {
    val key = "\"$field\":"
    val ki  = json.indexOf(key).takeIf { it >= 0 } ?: return ""
    var i   = ki + key.length
    while (i < json.length && json[i].isWhitespace()) i++
    if (i >= json.length || json[i] != '"') return ""
    i++ // skip opening quote
    val sb = StringBuilder()
    while (i < json.length) {
        when {
            json[i] == '\\' && i + 1 < json.length -> {
                i++
                when (json[i]) {
                    '"'  -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n'  -> sb.append('\n')
                    't'  -> sb.append('\t')
                    'r'  -> sb.append('\r')
                    else -> { sb.append('\\'); sb.append(json[i]) }
                }
            }
            json[i] == '"' -> return sb.toString() // complete value
            else -> sb.append(json[i])
        }
        i++
    }
    // Closing quote not yet received — return partial value for in-progress display
    return sb.toString()
}

// ── Confidence pill ───────────────────────────────────────────────────────────

@Composable
private fun ConfidencePill(confidence: String) {
    val colors = LocalPulseColors.current
    val (label, color) = when (confidence) {
        "high"              -> "High confidence" to colors.success
        "medium"            -> "Medium confidence" to colors.textMuted
        "low"               -> "Low confidence" to colors.warning
        "insufficient_data" -> "Insufficient data" to colors.warning
        else                -> confidence to colors.textMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

// ── Welcome card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeCard(onChipTap: (String) -> Unit) {
    val colors = LocalPulseColors.current
    val suggestions = listOf(
        "How's my heart rate trending this week?",
        "How did I sleep last night?",
        "Am I hitting my step goal?",
        "What's my SpO₂ average?"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(listOf(colors.accent, colors.spo2, colors.sleep, colors.accent)))
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text("Ask me anything about your health data", fontSize = 14.sp, color = colors.textSecondary, lineHeight = 20.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { s ->
                SuggestionChip(
                    onClick = { onChipTap(s) },
                    label = { Text(s, fontSize = 12.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colors.elevated,
                        labelColor = colors.textPrimary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = colors.borderStrong
                    )
                )
            }
        }
    }
}
