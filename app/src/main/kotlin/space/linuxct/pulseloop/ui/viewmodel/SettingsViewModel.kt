package space.linuxct.pulseloop.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore.Companion.DEFAULT_COACH_MODEL
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.data.network.createOAuthFlow
import space.linuxct.pulseloop.data.network.exchangeAuthorizationCode
import space.linuxct.pulseloop.data.network.waitForOAuthCode
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.service.MetricsService
import space.linuxct.pulseloop.update.UpdateCheckWorker
import space.linuxct.pulseloop.update.UpdateChecker
import javax.inject.Inject

data class SettingsUiState(
    val device: DeviceEntity? = null,
    val profile: UserProfileEntity? = null,
    val goals: UserGoalEntity? = null,
    val capabilities: Set<WearableCapability> = emptySet()
)

sealed class OAuthState {
    object Idle    : OAuthState()
    object Waiting : OAuthState()
    object Success : OAuthState()
    data class Error(val message: String) : OAuthState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepo: DeviceRepository,
    private val profileRepo: ProfileRepository,
    private val measurementRepo: MeasurementRepository,
    private val bleClient: RingBLEClient,
    private val prefs: AppPreferencesDataStore
) : ViewModel() {

    val connectionState: StateFlow<RingConnectionState> = bleClient.connectionState

    private val _isFindingDevice = MutableStateFlow(false)
    val isFindingDevice: StateFlow<Boolean> = _isFindingDevice.asStateFlow()

    val openAiKey: StateFlow<String?> = prefs.openAiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val coachModel: StateFlow<String> = prefs.coachModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_COACH_MODEL)

    val useMaterialYou: StateFlow<Boolean> = prefs.useMaterialYou
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Blood-pressure / blood-sugar calibration (Jring only — gated by capability in the UI).
    val bpCalSystolic: StateFlow<Int> = prefs.bpCalSystolic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val bpCalDiastolic: StateFlow<Int> = prefs.bpCalDiastolic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val glucoseOffsetMgdl: StateFlow<Double> = prefs.glucoseOffsetMgdl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val glucoseRefMgdl: StateFlow<Double> = prefs.glucoseRefMgdl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /**
     * Master opt-in for the ring's estimated blood pressure / blood sugar. Off by default.
     *
     * Backed by a SYNCHRONOUS MutableStateFlow (seeded once from DataStore) rather than a
     * DataStore-derived flow, so a tap flips it in the SAME frame and the V2 settings switch
     * recomposes immediately. Binding straight to the async DataStore round-trip made the V2
     * Switch lag a frame and only refresh once scrolled off-screen. DataStore is written in the
     * background. (Same pattern the OTLP text fields use in DataExportViewModel.)
     */
    private val _bloodMetricsEnabled = MutableStateFlow(false)
    val bloodMetricsEnabled: StateFlow<Boolean> = _bloodMetricsEnabled.asStateFlow()

    fun setBloodMetricsEnabled(enabled: Boolean) {
        _bloodMetricsEnabled.value = enabled                                  // instant UI update
        viewModelScope.launch { prefs.setBloodMetricsEnabled(enabled) }       // persist in background
    }

    init {
        viewModelScope.launch { prefs.initCoachModelDefault() }
        // Seed the synchronous opt-in flow once from the persisted value.
        viewModelScope.launch { _bloodMetricsEnabled.value = prefs.bloodMetricsEnabled.first() }
        // Reset find state when the ring disconnects so the button doesn't stay stuck
        viewModelScope.launch {
            bleClient.connectionState.collect { state ->
                if (state != RingConnectionState.CONNECTED) {
                    _isFindingDevice.value = false
                }
            }
        }
    }

    private val _oauthState = MutableStateFlow<OAuthState>(OAuthState.Idle)
    val oauthState: StateFlow<OAuthState> = _oauthState.asStateFlow()

    private val _launchBrowser = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val launchBrowser: SharedFlow<String> = _launchBrowser

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private var oauthJob: Job? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        deviceRepo.observeDevice(),
        profileRepo.observeProfile(),
        profileRepo.observeGoals()
    ) { device, profile, goals ->
        SettingsUiState(
            device = device, profile = profile, goals = goals,
            capabilities = MetricsService.deviceCapabilities(device)
        )
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
                _oauthState.value = OAuthState.Error(e.message ?: context.getString(R.string.settings_oauth_error_default))
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
            // Push the profile to the ring so its blood-sugar (profile-derived) estimate stays accurate.
            // No-op on Colmi. Also re-applied on every connect by RingStartupCoordinator.
            if (age != null && heightCm != null && weightKg != null) {
                bleClient.setUserInfo(
                    ageYears = age,
                    isMale = biologicalSex?.equals("male", ignoreCase = true) == true,
                    heightCm = heightCm,
                    weightKg = weightKg.toInt()
                )
            }
        }
    }

    /**
     * Save a reference cuff systolic/diastolic. Stored as a display offset AND pushed to the ring
     * (0x33) so it offsets its own readings. Jring only (no-op on Colmi).
     */
    fun setBpCalibration(systolic: Int, diastolic: Int) {
        viewModelScope.launch {
            prefs.setBpCalibration(systolic, diastolic)
            if (systolic in 1..300 && diastolic in 1..300) {
                bleClient.setBloodPressureAdjust(systolic, diastolic)
            }
        }
    }

    /**
     * Calibrate blood sugar against a reference lab/meter reading. The ring has no glucose-calibration
     * command, so this is a pure display offset: offset = reference − latest raw reading. Requires at
     * least one prior blood-sugar measurement; emits a snackbar otherwise.
     */
    fun calibrateGlucose(referenceMgdl: Double) {
        viewModelScope.launch {
            val latestRaw = measurementRepo.getLatest(MeasurementKind.BLOOD_SUGAR)?.value
            if (latestRaw == null) {
                _snackbarMessage.emit(context.getString(R.string.settings_glucose_calibrate_needs_reading))
                return@launch
            }
            prefs.setGlucoseCalibration(offsetMgdl = referenceMgdl - latestRaw, refMgdl = referenceMgdl)
        }
    }

    fun resetGlucoseCalibration() {
        viewModelScope.launch { prefs.clearGlucoseCalibration() }
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

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            when (val result = UpdateChecker.check(context)) {
                is UpdateChecker.Result.UpdateAvailable -> _launchBrowser.emit(UpdateCheckWorker.RELEASES_URL)
                is UpdateChecker.Result.NoUpdate -> _snackbarMessage.emit(context.getString(R.string.settings_update_not_found))
                is UpdateChecker.Result.Error -> _snackbarMessage.emit(context.getString(R.string.settings_update_error))
            }
            _isCheckingUpdate.value = false
        }
    }

    fun setCoachModel(model: String) {
        viewModelScope.launch { prefs.setCoachModel(model.trim()) }
    }

    fun setUseMaterialYou(enabled: Boolean) {
        viewModelScope.launch { prefs.setUseMaterialYou(enabled) }
    }

    fun syncNow() = bleClient.syncNow()

    fun toggleFindDevice() {
        if (_isFindingDevice.value) {
            _isFindingDevice.value = false
            bleClient.stopFindDevice()
        } else {
            _isFindingDevice.value = true
            bleClient.findDevice()
        }
    }

    fun disconnectDevice() = bleClient.disconnect()

    fun reconnectDevice() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) { bleClient.reconnect() }
        }
    }

    fun forgetDevice() {
        bleClient.disconnect()
        viewModelScope.launch { deviceRepo.deleteAll() }
    }
}
