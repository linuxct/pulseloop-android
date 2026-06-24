package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_sessions ORDER BY startAt DESC")
    suspend fun getAllSessions(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions ORDER BY startAt DESC")
    fun observeSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions ORDER BY startAt DESC LIMIT 1")
    suspend fun getLatestSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE startAt >= :midnightMs AND startAt < :nextMidnightMs LIMIT 1")
    suspend fun getSessionForDate(midnightMs: Long, nextMidnightMs: Long): SleepSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: SleepSessionEntity)

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_stage_blocks WHERE sessionId = :sessionId ORDER BY startAt ASC")
    suspend fun getBlocksForSession(sessionId: String): List<SleepStageBlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlock(block: SleepStageBlockEntity)

    @Query("SELECT startAt FROM sleep_stage_blocks WHERE sessionId = :sessionId")
    suspend fun getExistingBlockStartTimes(sessionId: String): List<Long>
}
