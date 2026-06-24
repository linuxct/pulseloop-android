package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.DerivedUpdateRowEntity
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity

interface DebugRepository {
    suspend fun insertRawPacket(packet: RawPacketRowEntity)
    fun observeRecentPackets(): Flow<List<RawPacketRowEntity>>
    suspend fun getAllPackets(): List<RawPacketRowEntity>
    suspend fun insertDerivedUpdate(update: DerivedUpdateRowEntity)
    fun observeRecentDerivedUpdates(): Flow<List<DerivedUpdateRowEntity>>
    suspend fun insertLog(log: WearableLogEntity)
    fun observeLogs(): Flow<List<WearableLogEntity>>
    suspend fun getAllLogs(): List<WearableLogEntity>
    suspend fun clearAll()
}
