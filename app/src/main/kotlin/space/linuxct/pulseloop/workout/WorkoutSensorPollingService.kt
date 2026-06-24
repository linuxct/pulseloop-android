package space.linuxct.pulseloop.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSensorPollingService @Inject constructor(
    private val bleClient: RingBLEClient,
    private val activityRepo: ActivityRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionId: String? = null
    private var loopJob: Job? = null
    private var isPolling = false

    private val hrIntervalMs    = 60_000L
    private val spo2IntervalMs  = 300_000L
    private val disconnectedRetryMs = 10_000L

    private var nextHRPollAt  = 0L
    private var nextSpo2PollAt = 0L

    var onPollCompleted: (() -> Unit)? = null

    fun start(sessionId: String) {
        this.sessionId = sessionId
        nextHRPollAt   = System.currentTimeMillis()
        nextSpo2PollAt = System.currentTimeMillis()
        launchLoop()
    }

    fun pause() {
        loopJob?.cancel()
        loopJob = null
    }

    fun resume() {
        if (sessionId == null) return
        nextHRPollAt   = System.currentTimeMillis()
        nextSpo2PollAt = System.currentTimeMillis()
        launchLoop()
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        sessionId = null
    }

    fun recoverIfNeeded(existingSessionId: String) {
        if (loopJob?.isActive == true) return
        sessionId = existingSessionId
        nextHRPollAt   = System.currentTimeMillis()
        nextSpo2PollAt = System.currentTimeMillis()
        launchLoop()
    }

    private fun launchLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()

                if (!isPolling && now >= nextHRPollAt) {
                    val connected = poll(kind = "hr")
                    nextHRPollAt = System.currentTimeMillis() + if (connected) hrIntervalMs else disconnectedRetryMs
                } else if (now >= nextHRPollAt) {
                    recordPoll("hr", "skipped", null, null)
                    nextHRPollAt = System.currentTimeMillis() + hrIntervalMs
                }

                if (!isPolling && now >= nextSpo2PollAt) {
                    val connected = poll(kind = "spo2")
                    nextSpo2PollAt = System.currentTimeMillis() + if (connected) spo2IntervalMs else disconnectedRetryMs
                } else if (now >= nextSpo2PollAt) {
                    recordPoll("spo2", "skipped", null, null)
                    nextSpo2PollAt = System.currentTimeMillis() + spo2IntervalMs
                }

                delay(5_000L)
            }
        }
    }

    private suspend fun poll(kind: String): Boolean {
        val sid = sessionId ?: return true
        val session = activityRepo.getSessionById(sid) ?: run {
            recordPoll(kind, "skipped", null, null)
            return true
        }
        if (session.statusRaw != "recording") {
            recordPoll(kind, "skipped", null, null)
            return true
        }
        if (bleClient.connectionState.value != RingConnectionState.CONNECTED) {
            recordPoll(kind, "skipped", "ring disconnected", null)
            return false
        }
        isPolling = true
        recordPoll(kind, "started", null, null)
        try {
            when (kind) {
                "hr"   -> bleClient.measureHR()
                "spo2" -> bleClient.measureSpO2()
            }
            recordPoll(kind, "success", null, null)
        } catch (e: Exception) {
            recordPoll(kind, "failed", e.message, null)
        } finally {
            isPolling = false
        }
        onPollCompleted?.invoke()
        return true
    }

    private suspend fun recordPoll(kind: String, status: String, error: String?, value: Double?) {
        val sid = sessionId ?: return
        activityRepo.insertPollEvent(
            ActivitySensorPollEventEntity(
                id           = newUUID(),
                sessionId    = sid,
                timestamp    = System.currentTimeMillis(),
                sensorKindRaw = kind,
                statusRaw    = status,
                resultValue  = value,
                errorMessage = error
            )
        )
    }
}
