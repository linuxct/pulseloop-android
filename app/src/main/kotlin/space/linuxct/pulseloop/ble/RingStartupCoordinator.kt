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
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RingStartupCoordinator"
private const val FOREGROUND_INTERVAL_MS = 60_000L
private const val BACKGROUND_INTERVAL_MS = 15 * 60 * 1_000L
// Time to let the ring finish sending sync response packets before we send
// the SpO2 start command — jring ignores 0x23 01 while busy with history data.
private const val SYNC_SETTLE_MS = 10_000L

@Singleton
class RingStartupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepo: DeviceRepository,
    private val bleClient: RingBLEClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    @Volatile private var isForeground = false

    fun start() {
        // Connection state → service lifecycle + sync loop
        scope.launch {
            bleClient.connectionState.collect { state ->
                when (state) {
                    RingConnectionState.CONNECTED -> {
                        Log.d(TAG, "Ring connected — starting foreground service")
                        context.startForegroundService(
                            Intent(context, RingConnectionService::class.java)
                        )
                        startSyncLoop(syncImmediately = true)
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

    private fun startSyncLoop(syncImmediately: Boolean) {
        val interval = if (isForeground) FOREGROUND_INTERVAL_MS else BACKGROUND_INTERVAL_MS
        val label = if (isForeground) "1-min" else "15-min"
        syncJob?.cancel()
        syncJob = scope.launch {
            if (syncImmediately) {
                Log.d(TAG, "Immediate sync ($label mode)")
                bleClient.syncNow()
                delay(SYNC_SETTLE_MS)
                triggerAutoSpO2()
            }
            while (true) {
                delay(interval)
                if (bleClient.connectionState.value == RingConnectionState.CONNECTED) {
                    Log.d(TAG, "$label sync tick")
                    bleClient.syncNow()
                    delay(SYNC_SETTLE_MS)
                    triggerAutoSpO2()
                }
            }
        }
    }

    private suspend fun triggerAutoSpO2() {
        if (bleClient.spO2Active) {
            Log.d(TAG, "SpO2 already in progress — skipping auto-trigger")
            return
        }
        if (bleClient.connectionState.value != RingConnectionState.CONNECTED) {
            Log.d(TAG, "Ring not connected — skipping SpO2 auto-trigger")
            return
        }
        Log.d(TAG, "Auto-triggering SpO2 measurement")
        bleClient.measureSpO2()
        // Wait up to 30 s for a result, then stop regardless
        withTimeoutOrNull(30_000L) {
            PulseEventBus.events.filter {
                it is PulseEvent.Spo2Result || it is PulseEvent.Spo2Complete
            }.first()
        }
        bleClient.stopSpO2()
        Log.d(TAG, "SpO2 auto-measurement complete")
    }

    private fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }
}
