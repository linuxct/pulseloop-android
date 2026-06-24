package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivityEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity

interface ActivityRepository {
    // Daily totals
    fun observeAll(): Flow<List<ActivityDailyEntity>>
    suspend fun getAll(): List<ActivityDailyEntity>
    suspend fun getForDate(midnightMs: Long): ActivityDailyEntity?
    suspend fun upsert(row: ActivityDailyEntity)
    suspend fun getSince(cutoffMs: Long): List<ActivityDailyEntity>

    // Sessions
    fun observeSessions(): Flow<List<ActivitySessionEntity>>
    suspend fun getAllSessions(): List<ActivitySessionEntity>
    suspend fun getSessionById(id: String): ActivitySessionEntity?
    suspend fun getActiveSession(): ActivitySessionEntity?
    suspend fun upsertSession(session: ActivitySessionEntity)
    suspend fun deleteSession(id: String)

    // Samples
    suspend fun insertSample(sample: ActivitySampleEntity)
    suspend fun getSamplesForSession(sessionId: String): List<ActivitySampleEntity>

    // GPS points
    suspend fun insertGpsPoint(point: ActivityGpsPointEntity)
    suspend fun getGpsPointsForSession(sessionId: String): List<ActivityGpsPointEntity>
    fun observeGpsPointsForSession(sessionId: String): Flow<List<ActivityGpsPointEntity>>

    // Poll events
    suspend fun insertPollEvent(event: ActivitySensorPollEventEntity)
    suspend fun getPollEventsForSession(sessionId: String): List<ActivitySensorPollEventEntity>

    // Activity events (audit log)
    suspend fun insertEvent(event: ActivityEventEntity)
    suspend fun getEventsForSession(sessionId: String): List<ActivityEventEntity>
}
