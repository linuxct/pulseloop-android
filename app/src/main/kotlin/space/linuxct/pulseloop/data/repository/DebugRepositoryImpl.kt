package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.DebugDao
import space.linuxct.pulseloop.data.db.entities.DerivedUpdateRowEntity
import space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity
import space.linuxct.pulseloop.data.db.entities.WearableLogEntity
import space.linuxct.pulseloop.domain.repository.DebugRepository
import javax.inject.Inject

class DebugRepositoryImpl @Inject constructor(
    private val dao: DebugDao
) : DebugRepository {
    override suspend fun insertRawPacket(packet: RawPacketRowEntity) {
        dao.insertRawPacket(packet)
        dao.prunePacketsToLimit()
    }
    override fun observeRecentPackets(): Flow<List<RawPacketRowEntity>> = dao.observeRecentPackets()
    override suspend fun getAllPackets() = dao.getAllPackets()
    override suspend fun insertDerivedUpdate(update: DerivedUpdateRowEntity) = dao.insertDerivedUpdate(update)
    override fun observeRecentDerivedUpdates(): Flow<List<DerivedUpdateRowEntity>> = dao.observeRecentDerivedUpdates()
    override suspend fun insertLog(log: WearableLogEntity) {
        dao.insertLog(log)
        dao.pruneLogsToLimit()
    }
    override fun observeLogs(): Flow<List<WearableLogEntity>> = dao.observeLogs()
    override suspend fun getAllLogs() = dao.getAllLogs()
    override suspend fun clearAll() {
        dao.deleteAllPackets()
        dao.deleteAllLogs()
    }
}
