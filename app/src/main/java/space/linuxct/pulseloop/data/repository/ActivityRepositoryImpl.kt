package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivityEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import javax.inject.Inject

class ActivityRepositoryImpl @Inject constructor(
    private val dailyDao: ActivityDailyDao,
    private val sessionDao: ActivitySessionDao
) : ActivityRepository {

    // Daily totals
    override fun observeAll(): Flow<List<ActivityDailyEntity>> = dailyDao.observeAll()
    override suspend fun getAll()                              = dailyDao.getAll()
    override suspend fun getForDate(midnightMs: Long)         = dailyDao.getForDate(midnightMs)
    override suspend fun upsert(row: ActivityDailyEntity)     = dailyDao.upsert(row)
    override suspend fun getSince(cutoffMs: Long)             = dailyDao.getSince(cutoffMs)

    // Sessions
    override fun observeSessions(): Flow<List<ActivitySessionEntity>> = sessionDao.observeAll()
    override suspend fun getAllSessions()                              = sessionDao.getAll()
    override suspend fun getSessionById(id: String)                   = sessionDao.getById(id)
    override suspend fun getActiveSession()                           = sessionDao.getActiveSession()
    override suspend fun upsertSession(session: ActivitySessionEntity) = sessionDao.upsert(session)
    override suspend fun deleteSession(id: String)                    = sessionDao.delete(id)

    // Samples
    override suspend fun insertSample(sample: ActivitySampleEntity)   = sessionDao.insertSample(sample)
    override suspend fun getSamplesForSession(sessionId: String)      = sessionDao.getSamplesForSession(sessionId)

    // GPS points
    override suspend fun insertGpsPoint(point: ActivityGpsPointEntity) = sessionDao.insertGpsPoint(point)
    override suspend fun getGpsPointsForSession(sessionId: String)     = sessionDao.getGpsPointsForSession(sessionId)
    override fun observeGpsPointsForSession(sessionId: String): Flow<List<ActivityGpsPointEntity>> =
        sessionDao.observeGpsPointsForSession(sessionId)

    // Poll events
    override suspend fun insertPollEvent(event: ActivitySensorPollEventEntity) = sessionDao.insertPollEvent(event)
    override suspend fun getPollEventsForSession(sessionId: String)            = sessionDao.getPollEventsForSession(sessionId)

    // Activity events
    override suspend fun insertEvent(event: ActivityEventEntity)      = sessionDao.insertEvent(event)
    override suspend fun getEventsForSession(sessionId: String)       = sessionDao.getEventsForSession(sessionId)
}
