package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.SleepDao
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.domain.repository.SleepRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SleepRepositoryImpl @Inject constructor(
    private val dao: SleepDao
) : SleepRepository {
    private val dayMs = TimeUnit.DAYS.toMillis(1)

    override suspend fun getAllSessions() = dao.getAllSessions()
    override fun observeSessions(): Flow<List<SleepSessionEntity>> = dao.observeSessions()
    override suspend fun getLatestSession() = dao.getLatestSession()
    override suspend fun getSessionForDate(midnightMs: Long) =
        dao.getSessionForDate(midnightMs, midnightMs + dayMs)
    override suspend fun upsertSession(session: SleepSessionEntity) = dao.upsertSession(session)
    override suspend fun updateSession(session: SleepSessionEntity) = dao.updateSession(session)
    override suspend fun getBlocksForSession(sessionId: String) = dao.getBlocksForSession(sessionId)
    override suspend fun insertBlock(block: SleepStageBlockEntity) = dao.insertBlock(block)
    override suspend fun getExistingBlockStartTimes(sessionId: String): Set<Long> =
        dao.getExistingBlockStartTimes(sessionId).toSet()
}
