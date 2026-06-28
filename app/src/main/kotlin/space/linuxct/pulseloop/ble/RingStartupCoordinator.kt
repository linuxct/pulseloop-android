package space.linuxct.pulseloop.ble

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.eventFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RingStartupCoordinator"
private const val FOREGROUND_INTERVAL_MS = 60_000L
private const val BACKGROUND_INTERVAL_MS = 15 * 60 * 1_000L
// Time to let the ring finish sending sync response packets before we send
// measurement commands — jring ignores 0x23/0x14 while busy with history data.
private const val SYNC_SETTLE_MS = 10_000L
// How often to trigger a spot HR measurement in each mode.
private const val FOREGROUND_HR_INTERVAL_MS = 3 * 60 * 1_000L
private const val BACKGROUND_HR_INTERVAL_MS = BACKGROUND_INTERVAL_MS

@Singleton
class RingStartupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepo: DeviceRepository,
    private val profileRepo: ProfileRepository,
    private val prefs: AppPreferencesDataStore,
    private val bleClient: RingBLEClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    @Volatile private var isForeground = false
    @Volatile private var lastHRTriggerMs = 0L

    fun start() {
        // Connection state → service lifecycle + sync loop
        scope.launch {
            bleClient.connectionState.collect { state ->
                when (state) {
                    RingConnectionState.CONNECTED -> {
                        Log.d(TAG, "Ring connected — starting foreground service")
                        try {
                            context.startForegroundService(
                                Intent(context, RingConnectionService::class.java)
                            )
                        } catch (e: Exception) {
                            // ForegroundServiceStartNotAllowedException: OS rejects the call when
                            // the app was cold-started from a background source and the allowance
                            // window has elapsed. The service will start on the next foreground event.
                            Log.w(TAG, "Could not start foreground service: ${e.message}")
                        }
                        startSyncLoop(syncImmediately = true, cleanupOnConnect = true)
                    }
                    RingConnectionState.DISCONNECTED,
                    RingConnectionState.IDLE -> {
                        Log.d(TAG, "Ring disconnected/idle — stopping service and sync loop")
                        context.stopService(Intent(context, RingConnectionService::class.java))
                        stopSyncLoop()
                    }
                    else -> Unit
                }
            }
        }

        // Auto-reconnect saved device on startup
        scope.launch {
            val device = deviceRepo.getDevice() ?: return@launch
            Log.d(TAG, "Found saved device ${device.macAddress}, resetting state and reconnecting")
            deviceRepo.upsert(device.copy(stateRaw = RingConnectionState.DISCONNECTED.rawValue))
            withContext(Dispatchers.Main) {
                bleClient.connectToAddress(device.macAddress, autoConnect = true)
            }
        }

        // Foreground/background detection via eventFlow — no Handler needed
        scope.launch {
            ProcessLifecycleOwner.get().lifecycle.eventFlow.collect { event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        Log.d(TAG, "App foregrounded — switching to 1-min sync")
                        isForeground = true
                        if (bleClient.connectionState.value == RingConnectionState.CONNECTED) {
                            startSyncLoop(syncImmediately = true)
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        Log.d(TAG, "App backgrounded — switching to 15-min sync")
                        isForeground = false
                        if (bleClient.connectionState.value == RingConnectionState.CONNECTED) {
                            startSyncLoop(syncImmediately = false)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun startSyncLoop(syncImmediately: Boolean, cleanupOnConnect: Boolean = false) {
        val interval = if (isForeground) FOREGROUND_INTERVAL_MS else BACKGROUND_INTERVAL_MS
        val label = if (isForeground) "1-min" else "15-min"
        syncJob?.cancel()
        lastHRTriggerMs = 0L  // reset so the first sync after connect/foreground always measures HR
        syncJob = scope.launch {
            if (syncImmediately) {
                if (cleanupOnConnect) {
                    // Ring may have been mid-HR or mid-SpO2 when it disconnected. Send stop
                    // commands first so the ring exits any stuck measurement loop before we
                    // send sync queries. The write queue serialises delivery, but the ring's
                    // firmware needs a moment to process the stops before new commands arrive.
                    Log.d(TAG, "Sending stop commands to clear any in-progress ring measurements")
                    bleClient.stopHR()
                    bleClient.stopSpO2()
                    delay(500L)
                }
                Log.d(TAG, "Immediate sync ($label mode)")
                bleClient.syncNow()
                bleClient.refreshBattery()
                if (cleanupOnConnect) pushUserSettings()
                delay(SYNC_SETTLE_MS)
                triggerAutoHR()
                triggerAutoCombinedVitals()
            }
            while (true) {
                delay(interval)
                if (bleClient.connectionState.value == RingConnectionState.CONNECTED) {
                    Log.d(TAG, "$label sync tick")
                    bleClient.syncNow()
                    bleClient.refreshBattery()
                    delay(SYNC_SETTLE_MS)
                    triggerAutoHR()
                    triggerAutoCombinedVitals()
                }
            }
        }
    }

    /**
     * Push the user's physical profile (0x02) and any blood-pressure calibration (0x33) to the ring
     * on connect, so its blood-sugar (profile-derived) estimate and BP offset are accurate. No-ops on
     * Colmi (the sync-engine methods default to no-op there). Mirrors the reference fork's
     * pushUserSettingsFromStore().
     */
    private suspend fun pushUserSettings() {
        val profile = profileRepo.getProfile()
        val age = profile?.age
        val heightCm = profile?.heightCm
        val weightKg = profile?.weightKg?.toInt()
        if (age != null && heightCm != null && weightKg != null) {
            val isMale = profile.biologicalSex?.equals("male", ignoreCase = true) == true
            Log.d(TAG, "Pushing user profile to ring (age=$age, male=$isMale, h=$heightCm, w=$weightKg)")
            bleClient.setUserInfo(age, isMale, heightCm, weightKg)
        }
        val sys = prefs.bpCalSystolic.first()
        val dia = prefs.bpCalDiastolic.first()
        if (sys in 1..300 && dia in 1..300) {
            Log.d(TAG, "Pushing BP calibration to ring (sys=$sys, dia=$dia)")
            bleClient.setBloodPressureAdjust(sys, dia)
        }
    }

    private suspend fun triggerAutoHR() {
        val hrInterval = if (isForeground) FOREGROUND_HR_INTERVAL_MS else BACKGROUND_HR_INTERVAL_MS
        val now = System.currentTimeMillis()
        if (now - lastHRTriggerMs < hrInterval) return
        if (bleClient.hrActive) {
            Log.d(TAG, "HR already in progress — skipping auto-trigger")
            return
        }
        if (bleClient.connectionState.value != RingConnectionState.CONNECTED) return

        Log.d(TAG, "Auto-triggering HR measurement (${if (isForeground) "3-min fg" else "15-min bg"})")
        lastHRTriggerMs = now
        bleClient.measureHR()
        // Wait up to 30s for the first HR sample, or exit early if the ring signals
        // it finished without a reading (HeartRateComplete = 0x27, e.g. ring not worn).
        withTimeoutOrNull(30_000L) {
            PulseEventBus.events.filter {
                it is PulseEvent.HeartRateSample || it is PulseEvent.HeartRateComplete
            }.first()
        }
        bleClient.stopHR()
        Log.d(TAG, "HR auto-measurement complete")
    }

    /**
     * Every sync pulls the on-demand vitals after HR. On the Jring the SpO2 command (0x23) returns a
     * COMBINED 0x24 reading — SpO2 + blood pressure + blood sugar + fatigue + stress in one response —
     * so a single trigger records all of them together. Colmi has no combined command, so it falls
     * back to its big-data SpO2 request (its stress/HRV/temp come from the startup history sweep).
     */
    private suspend fun triggerAutoCombinedVitals() {
        if (bleClient.spO2Active) {
            Log.d(TAG, "SpO2/combined already in progress — skipping auto-trigger")
            return
        }
        if (bleClient.connectionState.value != RingConnectionState.CONNECTED) {
            Log.d(TAG, "Ring not connected — skipping combined-vitals auto-trigger")
            return
        }
        val supportsCombined = bleClient.capabilities.value.contains(WearableCapability.BLOOD_PRESSURE)
        if (supportsCombined) {
            Log.d(TAG, "Auto-triggering combined vitals (SpO2 + BP + blood sugar + fatigue + stress)")
            bleClient.measureCombined()
        } else {
            Log.d(TAG, "Auto-triggering SpO2 measurement")
            bleClient.measureSpO2()
        }
        // Wait up to 30 s for a result, then stop regardless. The 0x24 combined response surfaces as
        // Spo2Result/Spo2Complete, and the BP/sugar/fatigue/stress rows persist on arrival regardless.
        withTimeoutOrNull(30_000L) {
            PulseEventBus.events.filter {
                it is PulseEvent.Spo2Result || it is PulseEvent.Spo2Complete
            }.first()
        }
        bleClient.stopSpO2()
        Log.d(TAG, "Combined-vitals auto-measurement complete")
    }

    private fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }
}
