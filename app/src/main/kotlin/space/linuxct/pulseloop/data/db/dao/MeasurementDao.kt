package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity

@Dao
interface MeasurementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements WHERE kindRaw = :kind ORDER BY timestamp DESC")
    suspend fun getByKind(kind: String): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE kindRaw = :kind AND timestamp >= :cutoffMs ORDER BY timestamp DESC")
    suspend fun getByKindSince(kind: String, cutoffMs: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE kindRaw = :kind ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(kind: String): MeasurementEntity?

    @Query("SELECT * FROM measurements WHERE kindRaw = :kind ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(kind: String): Flow<MeasurementEntity?>

    @Query("SELECT * FROM measurements WHERE timestamp >= :cutoffMs ORDER BY timestamp DESC")
    fun observeSince(cutoffMs: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE activitySessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<MeasurementEntity>

    @Query("DELETE FROM measurements WHERE timestamp < :cutoffMs")
    suspend fun pruneOlderThan(cutoffMs: Long)
}
