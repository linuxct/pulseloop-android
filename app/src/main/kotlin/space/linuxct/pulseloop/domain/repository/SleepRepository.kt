package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity

interface SleepRepository {
    suspend fun getAllSessions(): List<SleepSessionEntity>
    fun observeSessions(): Flow<List<SleepSessionEntity>>
    suspend fun getLatestSession(): SleepSessionEntity?
    suspend fun getSessionForDate(midnightMs: Long): SleepSessionEntity?
    suspend fun upsertSession(session: SleepSessionEntity)
    suspend fun updateSession(session: SleepSessionEntity)
    suspend fun getBlocksForSession(sessionId: String): List<SleepStageBlockEntity>
    suspend fun insertBlock(block: SleepStageBlockEntity)
    suspend fun getExistingBlockStartTimes(sessionId: String): Set<Long>
}
