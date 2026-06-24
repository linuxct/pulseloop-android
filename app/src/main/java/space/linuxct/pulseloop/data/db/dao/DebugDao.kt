package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.DerivedUpdateRowEntity
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity

@Dao
interface DebugDao {

    // Raw packets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawPacket(packet: RawPacketRowEntity)

    @Query("SELECT * FROM raw_packets ORDER BY timestamp DESC LIMIT 200")
    fun observeRecentPackets(): Flow<List<RawPacketRowEntity>>

    @Query("SELECT * FROM raw_packets ORDER BY timestamp DESC LIMIT 200")
    suspend fun getAllPackets(): List<RawPacketRowEntity>

    @Query("DELETE FROM raw_packets WHERE id NOT IN (SELECT id FROM raw_packets ORDER BY timestamp DESC LIMIT 200)")
    suspend fun prunePacketsToLimit()

    @Query("DELETE FROM raw_packets")
    suspend fun deleteAllPackets()

    // Derived updates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDerivedUpdate(update: DerivedUpdateRowEntity)

    @Query("SELECT * FROM derived_updates ORDER BY timestamp DESC LIMIT 200")
    fun observeRecentDerivedUpdates(): Flow<List<DerivedUpdateRowEntity>>

    // Wearable logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WearableLogEntity)

    @Query("SELECT * FROM wearable_logs ORDER BY at DESC LIMIT 100")
    fun observeLogs(): Flow<List<WearableLogEntity>>

    @Query("SELECT * FROM wearable_logs ORDER BY at DESC LIMIT 100")
    suspend fun getAllLogs(): List<WearableLogEntity>

    @Query("DELETE FROM wearable_logs WHERE id NOT IN (SELECT id FROM wearable_logs ORDER BY at DESC LIMIT 100)")
    suspend fun pruneLogsToLimit()

    @Query("DELETE FROM wearable_logs")
    suspend fun deleteAllLogs()
}
