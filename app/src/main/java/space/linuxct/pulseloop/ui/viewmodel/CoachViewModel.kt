package space.linuxct.pulseloop.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.coach.CoachOrchestrator
import space.linuxct.pulseloop.coach.CoachPromptBuilder
import space.linuxct.pulseloop.coach.model.CoachResponse
import space.linuxct.pulseloop.coach.tools.ToolContext
import space.linuxct.pulseloop.coach.tools.ToolRegistry
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.dao.CoachDao
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.dao.ProfileDao
import space.linuxct.pulseloop.data.db.dao.SleepDao
import space.linuxct.pulseloop.data.db.entities.CoachConversationEntity
import space.linuxct.pulseloop.data.db.entities.CoachMessageEntity
import space.linuxct.pulseloop.data.network.OpenAIClient
import java.util.UUID
import javax.inject.Inject

data class CoachMessageUi(
    val id: String,
    val role: String,
    val text: String,
    val response: CoachResponse?,
    val timestamp: Long
)

data class CoachUiState(
    val messages: List<CoachMessageUi> = emptyList(),
    val isLoading: Boolean = false,
    val traceEvents: List<String> = emptyList(),
    val hasApiKey: Boolean = false,
    val conversationId: String? = null,
    val error: String? = null,
    // Non-null while the model streams its text response; grows one delta at a time.
    val streamingText: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachDao: CoachDao,
    private val measurementDao: MeasurementDao,
    private val activityDailyDao: ActivityDailyDao,
    private val sleepDao: SleepDao,
    private val activitySessionDao: ActivitySessionDao,
    private val profileDao: ProfileDao,
    private val prefs: AppPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState

    private val toolCtx: ToolContext by lazy {
        ToolContext(measurementDao, activityDailyDao, sleepDao, activitySessionDao, profileDao, coachDao)
    }

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.openAiKey.collect { key ->
                _uiState.update { it.copy(hasApiKey = !key.isNullOrBlank()) }
            }
        }
        viewModelScope.launch { ensureConversation() }
    }

    private suspend fun ensureConversation() {
        val existing = coachDao.observeConversations().first().firstOrNull()
        val convId = existing?.id ?: createConversation()
        attachConversation(convId)
    }

    private suspend fun createConversation(): String {
        val id  = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        coachDao.upsertConversation(
            CoachConversationEntity(id = id, title = null, createdAt = now, updatedAt = now, lastResponseId = null)
        )
        return id
    }

    private fun attachConversation(convId: String) {
        // Only clear messages when the conversation actually changes, to avoid a WelcomeCard flash.
        if (_uiState.value.conversationId != convId) {
            _uiState.update { it.copy(conversationId = convId, messages = emptyList()) }
        } else {
            _uiState.update { it.copy(conversationId = convId) }
        }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            try {
                coachDao.observeMessages(convId).collect { entities ->
                    _uiState.update { state -> state.copy(messages = entities.map { it.toUi() }) }
                }
            } catch (e: Exception) {
                Log.e("CoachViewModel", "messagesJob crashed for convId=$convId: ${e.message}", e)
            }
        }
    }

    fun send(userText: String) {
        val convId = _uiState.value.conversationId ?: return
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val apiKey = prefs.openAiKey.first()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Log in with OpenAI in Settings → AI Coach") }
                return@launch
            }

            val now = System.currentTimeMillis()
            coachDao.insertMessage(
                CoachMessageEntity(
                    id = UUID.randomUUID().toString(), conversationId = convId,
                    roleRaw = "user", textContent = userText, cardsJson = null,
                    pendingActionJson = null, timestamp = now,
                    responseId = null, modelUsed = null, confidenceRaw = null
                )
            )

            _uiState.update { it.copy(isLoading = true, traceEvents = emptyList(), error = null) }

            val profile     = profileDao.getProfile()
            val goals       = profileDao.getGoals()
            val contextJson = CoachPromptBuilder.buildContext(profile, goals)
            val history     = coachDao.getRecentMessages(convId, 12).reversed()
                .filter { it.roleRaw == "user" || it.roleRaw == "assistant" }

            val isOAuth  = !prefs.openAiRefreshToken.first().isNullOrBlank()
            val client   = if (isOAuth) OpenAIClient.forOAuth(apiKey) else OpenAIClient(apiKey)
            val model    = prefs.coachModel.first() ?: "gpt-5.4"
            val orchestrator = CoachOrchestrator(
                client   = client,
                tools    = ToolRegistry.build(toolCtx),
                toolCtx  = toolCtx,
                model    = model,
                onTrace  = { event -> _uiState.update { it.copy(traceEvents = it.traceEvents + event) } }
            )

            val result = try {
                orchestrator.runTurn(
                    systemPrompt        = CoachPromptBuilder.systemPrompt,
                    developerMessage    = CoachPromptBuilder.buildDeveloperMessage(contextJson),
                    userText            = userText,
                    conversationHistory = history,
                    onTextDelta         = { delta ->
                        _uiState.update { s ->
                            // Clear tool trace events on the first delta so the bubble transitions
                            // cleanly from the "Checking…" chip row to streaming prose.
                            s.copy(
                                streamingText = (s.streamingText ?: "") + delta,
                                traceEvents   = if (s.streamingText == null) emptyList() else s.traceEvents
                            )
                        }
                    }
                )
            } finally {
                // Always clear the streaming buffer. The final persisted message replaces it,
                // or on failure we don't want a ghost streaming bubble left on screen.
                _uiState.update { it.copy(streamingText = null) }
            }

            when (result) {
                is CoachOrchestrator.TurnResult.Success -> {
                    val replyNow = System.currentTimeMillis()
                    try {
                        coachDao.insertMessage(
                            CoachMessageEntity(
                                id = UUID.randomUUID().toString(), conversationId = convId,
                                roleRaw = "assistant",
                                textContent  = result.response.summary,
                                cardsJson    = result.rawJson,
                                pendingActionJson = null,
                                timestamp    = replyNow,
                                responseId   = result.responseId,
                                modelUsed    = model,
                                confidenceRaw = result.response.confidence
                            )
                        )
                        val conv = coachDao.getConversation(convId)
                        if (conv != null) {
                            // @Update (not upsertConversation) — INSERT OR REPLACE would CASCADE-delete
                            // all messages via the FK before re-inserting the conversation row.
                            coachDao.updateConversation(
                                conv.copy(lastResponseId = result.responseId, updatedAt = replyNow)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CoachViewModel", "Failed to persist assistant message: ${e.message}", e)
                    } finally {
                        _uiState.update { it.copy(isLoading = false, traceEvents = emptyList()) }
                    }
                }
                is CoachOrchestrator.TurnResult.Failure -> {
                    Log.e("CoachViewModel", "TurnResult.Failure: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, traceEvents = emptyList(), error = result.message) }
                }
            }
        }
    }

    fun newConversation() {
        viewModelScope.launch {
            val id = createConversation()
            attachConversation(id)
        }
    }

    fun clearHistory() {
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            coachDao.deleteMessagesForConversation(convId)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun CoachMessageEntity.toUi(): CoachMessageUi {
        val parsed = cardsJson?.let { json ->
            try { CoachResponse.fromJson(json) }
            catch (e: Exception) { Log.w("CoachViewModel", "CoachResponse parse failed msg=$id: ${e.message}"); null }
        }
        return CoachMessageUi(
            id        = id,
            role      = roleRaw,
            text      = textContent ?: parsed?.summary ?: "",
            response  = parsed,
            timestamp = timestamp
        )
    }
}
