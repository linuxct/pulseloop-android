package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.data.network.createOAuthFlow
import space.linuxct.pulseloop.data.network.exchangeAuthorizationCode
import space.linuxct.pulseloop.data.network.waitForOAuthCode
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import javax.inject.Inject

data class SettingsUiState(
    val device: DeviceEntity? = null,
    val profile: UserProfileEntity? = null,
    val goals: UserGoalEntity? = null
)

sealed class OAuthState {
    object Idle    : OAuthState()
    object Waiting : OAuthState()
    object Success : OAuthState()
    data class Error(val message: String) : OAuthState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val profileRepo: ProfileRepository,
    private val measurementRepo: MeasurementRepository,
    private val bleClient: RingBLEClient,
    private val prefs: AppPreferencesDataStore
) : ViewModel() {

    val connectionState: StateFlow<RingConnectionState> = bleClient.connectionState

    val openAiKey: StateFlow<String?> = prefs.openAiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _oauthState = MutableStateFlow<OAuthState>(OAuthState.Idle)
    val oauthState: StateFlow<OAuthState> = _oauthState.asStateFlow()

    private val _launchBrowser = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val launchBrowser: SharedFlow<String> = _launchBrowser

    private var oauthJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        deviceRepo.observeDevice(),
        profileRepo.observeProfile(),
        profileRepo.observeGoals()
    ) { device, profile, goals ->
        SettingsUiState(device = device, profile = profile, goals = goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun startOpenAIOAuth() {
        oauthJob?.cancel()
        oauthJob = viewModelScope.launch {
            _oauthState.value = OAuthState.Waiting
            try {
                val flow = createOAuthFlow()
                _launchBrowser.emit(flow.url)
                val code = waitForOAuthCode(flow.state)
                val tokens = exchangeAuthorizationCode(code, flow.verifier)
                prefs.setOpenAiTokens(tokens.accessToken, tokens.refreshToken)
                _oauthState.value = OAuthState.Success
            } catch (e: Exception) {
                _oauthState.value = OAuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun cancelOAuth() {
        oauthJob?.cancel()
        oauthJob = null
        _oauthState.value = OAuthState.Idle
    }

    fun dismissOAuthResult() {
        _oauthState.value = OAuthState.Idle
    }

    fun clearOpenAiAuth() {
        viewModelScope.launch { prefs.clearOpenAiAuth() }
    }

    fun updateProfile(name: String?, age: Int?, biologicalSex: String?, weightKg: Double?, heightCm: Int?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = profileRepo.getProfile()
            val updated = existing?.copy(
                name = name, age = age, biologicalSex = biologicalSex,
                weightKg = weightKg, heightCm = heightCm, updatedAt = now
            ) ?: UserProfileEntity(
                name = name, age = age, heightCm = heightCm, weightKg = weightKg,
                biologicalSex = biologicalSex, onboardingCompleted = true,
                createdAt = now, updatedAt = now
            )
            profileRepo.upsertProfile(updated)
        }
    }

    fun updateGoals(steps: Int, sleepMinutes: Int, activeMinutes: Int) {
        viewModelScope.launch {
            val existing = profileRepo.getGoals()
            val now = System.currentTimeMillis()
            if (existing != null) {
                profileRepo.upsertGoals(existing.copy(dailySteps = steps, sleepMinutes = sleepMinutes, activeMinutes = activeMinutes, updatedAt = now))
            } else {
                profileRepo.upsertGoals(UserGoalEntity(dailySteps = steps, sleepMinutes = sleepMinutes, activeMinutes = activeMinutes, updatedAt = now))
            }
        }
    }

    fun syncNow() = bleClient.syncNow()

    fun forgetDevice() {
        bleClient.disconnect()
        viewModelScope.launch { deviceRepo.deleteAll() }
    }
}
