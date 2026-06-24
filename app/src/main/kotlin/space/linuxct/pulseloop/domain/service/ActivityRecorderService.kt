package space.linuxct.pulseloop.domain.service

import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.data.db.entities.ActivityEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivitySessionStatus
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.MeasurementSource
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecorderService @Inject constructor(
    private val activityRepo: ActivityRepository
) {

    suspend fun start(type: String, useGps: Boolean, notes: String?): ActivitySessionEntity {
        val now = System.currentTimeMillis()
        val session = ActivitySessionEntity(
            id = newUUID(), activityType = type,
            statusRaw = ActivitySessionStatus.RECORDING.rawValue,
            startedAt = now, pausedAt = null, finishedAt = null,
            useGps = useGps, totalSteps = 0, totalCalories = 0.0, totalDistanceMeters = 0.0,
            totalActiveMinutes = 0, hrPollCount = 0, hrPollSuccessCount = 0,
            spo2PollCount = 0, spo2PollSuccessCount = 0, elapsedPausedMs = 0L, notes = notes
        )
        activityRepo.upsertSession(session)
        event(session.id, "created", now)
        event(session.id, "started", now)
        return session
    }

    suspend fun pause(session: ActivitySessionEntity) {
        if (session.statusRaw != ActivitySessionStatus.RECORDING.rawValue) return
        val now = System.currentTimeMillis()
        activityRepo.upsertSession(session.copy(statusRaw = ActivitySessionStatus.PAUSED.rawValue, pausedAt = now))
        event(session.id, "paused", now)
        event(session.id, "gps_stopped", now)
    }

    suspend fun resume(session: ActivitySessionEntity) {
        if (session.statusRaw != ActivitySessionStatus.PAUSED.rawValue) return
        val now = System.currentTimeMillis()
        val pausedMs = session.pausedAt?.let { now - it } ?: 0L
        activityRepo.upsertSession(session.copy(
            statusRaw = ActivitySessionStatus.RECORDING.rawValue,
            pausedAt = null,
            elapsedPausedMs = session.elapsedPausedMs + pausedMs
        ))
        event(session.id, "resumed", now)
        event(session.id, "gps_started", now)
    }

    suspend fun finish(session: ActivitySessionEntity) {
        val now = System.currentTimeMillis()
        val samples   = activityRepo.getSamplesForSession(session.id)
        val gpsPoints = activityRepo.getGpsPointsForSession(session.id)
        val summary   = ActivityService.buildSessionSummary(session, samples, gpsPoints, now)
        activityRepo.upsertSession(session.copy(
            statusRaw = ActivitySessionStatus.FINISHED.rawValue,
            finishedAt = now,
            totalDistanceMeters = summary.distanceMeters ?: session.totalDistanceMeters,
            totalCalories = summary.calories ?: session.totalCalories
        ))
        event(session.id, "gps_stopped", now)
        event(session.id, "finished", now)
    }

    suspend fun cancel(session: ActivitySessionEntity) {
        val now = System.currentTimeMillis()
        activityRepo.upsertSession(session.copy(statusRaw = ActivitySessionStatus.CANCELLED.rawValue, finishedAt = now))
        event(session.id, "gps_stopped", now)
        event(session.id, "cancelled", now)
    }

    suspend fun delete(session: ActivitySessionEntity) = activityRepo.deleteSession(session.id)

    suspend fun linkSample(
        sessionId: String,
        kind: MeasurementKind,
        value: Double,
        timestamp: Long,
        measurementId: String?,
        source: MeasurementSource
    ): ActivitySampleEntity? {
        val existing = activityRepo.getSamplesForSession(sessionId)
        if (measurementId != null && existing.any { it.measurementId == measurementId }) return null
        val sample = ActivitySampleEntity(
            id = newUUID(), sessionId = sessionId, measurementId = measurementId,
            kindRaw = kind.rawValue, value = value,
            unit = if (kind == MeasurementKind.HEART_RATE) "bpm" else "%",
            timestamp = timestamp, sourceRaw = source.rawValue
        )
        activityRepo.insertSample(sample)
        event(sessionId, "sensor_sample_linked", timestamp)
        return sample
    }

    suspend fun insertGpsPoint(point: ActivityGpsPointEntity) = activityRepo.insertGpsPoint(point)
    suspend fun insertPollEvent(event: ActivitySensorPollEventEntity) = activityRepo.insertPollEvent(event)

    suspend fun recoverStaleSessions(): List<ActivitySessionEntity> {
        val cutoffMs = System.currentTimeMillis() - 6 * 3_600_000L
        return activityRepo.getAllSessions().filter { s ->
            (s.statusRaw == ActivitySessionStatus.RECORDING.rawValue ||
             s.statusRaw == ActivitySessionStatus.PAUSED.rawValue) && s.startedAt < cutoffMs
        }
    }

    private suspend fun event(sessionId: String, type: String, timestamp: Long) {
        activityRepo.insertEvent(ActivityEventEntity(
            id = newUUID(), sessionId = sessionId,
            timestamp = timestamp, eventType = type, detail = null
        ))
    }
}
