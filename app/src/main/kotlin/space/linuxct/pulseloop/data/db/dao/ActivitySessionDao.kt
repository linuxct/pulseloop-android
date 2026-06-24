package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.ActivityEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySensorPollEventEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity

@Dao
interface ActivitySessionDao {

    @Query("SELECT * FROM activity_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<ActivitySessionEntity>

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActivitySessionEntity?

    @Query("SELECT * FROM activity_sessions WHERE statusRaw = 'recording' OR statusRaw = 'paused' LIMIT 1")
    suspend fun getActiveSession(): ActivitySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ActivitySessionEntity)

    @Query("DELETE FROM activity_sessions WHERE id = :id")
    suspend fun delete(id: String)

    // Samples
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSample(sample: ActivitySampleEntity)

    @Query("SELECT * FROM activity_samples WHERE sessionId = :sessionId")
    suspend fun getSamplesForSession(sessionId: String): List<ActivitySampleEntity>

    // GPS points
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpsPoint(point: ActivityGpsPointEntity)

    @Query("SELECT * FROM activity_gps_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getGpsPointsForSession(sessionId: String): List<ActivityGpsPointEntity>

    @Query("SELECT * FROM activity_gps_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeGpsPointsForSession(sessionId: String): Flow<List<ActivityGpsPointEntity>>

    // Sensor polls
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPollEvent(event: ActivitySensorPollEventEntity)

    @Query("SELECT * FROM activity_sensor_polls WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPollEventsForSession(sessionId: String): List<ActivitySensorPollEventEntity>

    // Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity)

    @Query("SELECT * FROM activity_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: String): List<ActivityEventEntity>
}
