package space.linuxct.pulseloop.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.service.ActivityRecorderService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class WorkoutLiveState(
    val sessionId: String,
    val activityType: String,
    val status: String,
    val elapsedSeconds: Int,
    val distanceMeters: Double,
    val latestHR: Int?,
    val latestSpO2: Int?,
    val paceSecondsPerKm: Double?
)

@Singleton
class LiveWorkoutManager @Inject constructor(
    private val bleClient: RingBLEClient,
    val gps: GpsRouteRecorder,
    private val polling: WorkoutSensorPollingService,
    private val activityRepo: ActivityRepository,
    private val recorderService: ActivityRecorderService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingDeepLinkSession = MutableStateFlow<String?>(null)
    val pendingDeepLinkSession: StateFlow<String?> = _pendingDeepLinkSession.asStateFlow()

    private val _liveState = MutableStateFlow<WorkoutLiveState?>(null)
    val liveState: StateFlow<WorkoutLiveState?> = _liveState.asStateFlow()

    private val _latestHR = MutableStateFlow<Int?>(null)
    val latestHR: StateFlow<Int?> = _latestHR.asStateFlow()

    private val _latestSpO2 = MutableStateFlow<Int?>(null)
    val latestSpO2: StateFlow<Int?> = _latestSpO2.asStateFlow()

    private var lastPushedDistanceM = 0.0
    private var lastPushAt = 0L

    init {
        polling.onPollCompleted = { scope.launch { refreshForActiveSession() } }
        scope.launch {
            PulseEventBus.events.collect { event ->
                when (event) {
                    is PulseEvent.HeartRateSample -> _latestHR.value = event.bpm
                    is PulseEvent.Spo2Result      -> _latestSpO2.value = event.value
                    is PulseEvent.GpsPoint -> recorderService.insertGpsPoint(
                        ActivityGpsPointEntity(
                            id = newUUID(),
                            sessionId = event.sessionId,
                            timestamp = event.timestamp,
                            latitude = event.latitude,
                            longitude = event.longitude,
                            altitude = event.altitude,
                            accuracy = event.accuracy,
                            speed = event.speed,
                            bearing = event.bearing,
                            accepted = event.accepted,
                            rejectionReason = event.rejectionReason
                        )
                    )
                    else -> Unit
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun start(type: String, useGps: Boolean): ActivitySessionEntity {
        activityRepo.getAllSessions()
            .filter { it.statusRaw == "recording" || it.statusRaw == "paused" }
            .forEach { recorderService.cancel(it) }
        polling.stop()
        gps.stop()

        val session = recorderService.start(type = type, useGps = useGps, notes = null)
        if (useGps) gps.start(sessionId = session.id, activityType = type)
        polling.start(sessionId = session.id)

        lastPushedDistanceM = 0.0
        lastPushAt = System.currentTimeMillis()
        pushState(session, status = "recording", force = true)
        return session
    }

    suspend fun pause(session: ActivitySessionEntity) {
        recorderService.pause(session)
        gps.stop()
        polling.pause()
        pushState(session, status = "paused", force = true)
    }

    suspend fun resume(session: ActivitySessionEntity) {
        recorderService.resume(session)
        if (session.useGps) gps.start(sessionId = session.id, activityType = session.activityType)
        polling.resume()
        pushState(session, status = "recording", force = true)
    }

    suspend fun finish(session: ActivitySessionEntity) {
        gps.stop()
        polling.stop()
        recorderService.finish(session)
        _liveState.value = null
    }

    suspend fun cancel(session: ActivitySessionEntity) {
        gps.stop()
        polling.stop()
        recorderService.cancel(session)
        _liveState.value = null
    }

    // ── State sync ────────────────────────────────────────────────────────────

    suspend fun syncState(session: ActivitySessionEntity) {
        pushState(session, status = if (session.statusRaw == "paused") "paused" else "recording", force = false)
    }

    private suspend fun pushState(session: ActivitySessionEntity, status: String, force: Boolean) {
        val distance = if (session.useGps) acceptedDistance(session.id) else 0.0
        val now = System.currentTimeMillis()
        val movedEnough = abs(distance - lastPushedDistanceM) >= 30.0
        val timeEnough  = (now - lastPushAt) >= 20_000L
        if (!force && !movedEnough && !timeEnough) return
        lastPushedDistanceM = distance
        lastPushAt = now

        val elapsedSec = elapsedSeconds(session)
        _liveState.value = WorkoutLiveState(
            sessionId       = session.id,
            activityType    = session.activityType,
            status          = status,
            elapsedSeconds  = elapsedSec,
            distanceMeters  = distance,
            latestHR        = _latestHR.value,
            latestSpO2      = _latestSpO2.value,
            paceSecondsPerKm = paceSecondsPerKm(distance, elapsedSec)
        )
    }

    private suspend fun refreshForActiveSession() {
        val session = activityRepo.getAllSessions()
            .firstOrNull { it.statusRaw == "recording" } ?: return
        pushState(session, status = "recording", force = true)
    }

    // ── Recovery ──────────────────────────────────────────────────────────────

    suspend fun recover() {
        val active = activityRepo.getAllSessions()
            .firstOrNull { it.statusRaw == "recording" } ?: return
        if (!isFreshlyActive(active)) return
        if (active.useGps && !gps.isTracking.value) {
            gps.start(sessionId = active.id, activityType = active.activityType)
        }
        polling.recoverIfNeeded(active.id)
        pushState(active, status = "recording", force = true)
    }

    suspend fun ensureActive(session: ActivitySessionEntity) {
        if (session.statusRaw != "recording") return
        if (session.useGps && !gps.isTracking.value) {
            gps.start(sessionId = session.id, activityType = session.activityType)
        }
        polling.recoverIfNeeded(session.id)
        pushState(session, status = "recording", force = true)
    }

    private fun isFreshlyActive(session: ActivitySessionEntity): Boolean =
        System.currentTimeMillis() - session.startedAt < 180_000L

    // ── Deep link ─────────────────────────────────────────────────────────────

    fun requestOpen(sessionId: String) { _pendingDeepLinkSession.value = sessionId }
    fun clearDeepLink() { _pendingDeepLinkSession.value = null }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun acceptedDistance(sessionId: String): Double {
        val points = activityRepo.getGpsPointsForSession(sessionId).filter { it.accepted }
        if (points.size < 2) return 0.0
        return points.zipWithNext().sumOf { (a, b) ->
            haversine(a.latitude, a.longitude, b.latitude, b.longitude)
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dPhi    = (lat2 - lat1) * Math.PI / 180
        val dLambda = (lon2 - lon1) * Math.PI / 180
        val p1 = lat1 * Math.PI / 180
        val p2 = lat2 * Math.PI / 180
        val h = sin(dPhi / 2) * sin(dPhi / 2) + cos(p1) * cos(p2) * sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * r * asin(sqrt(h).coerceAtMost(1.0))
    }

    private fun elapsedSeconds(session: ActivitySessionEntity): Int {
        val end = session.finishedAt ?: System.currentTimeMillis()
        return maxOf(0, ((end - session.startedAt - session.elapsedPausedMs) / 1000L).toInt())
    }

    private fun paceSecondsPerKm(distanceMeters: Double, durationSeconds: Int): Double? {
        if (distanceMeters < 50 || durationSeconds <= 0) return null
        return durationSeconds.toDouble() / (distanceMeters / 1000.0)
    }
}
