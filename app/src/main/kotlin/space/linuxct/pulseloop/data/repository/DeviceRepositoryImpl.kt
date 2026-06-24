package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.DeviceDao
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.domain.model.WearableCapability.Companion.fromCsv
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val dao: DeviceDao
) : DeviceRepository {
    override fun observeDevice(): Flow<DeviceEntity?> = dao.observeDevice()
    override suspend fun getDevice(): DeviceEntity? = dao.getDevice()
    override suspend fun upsert(device: DeviceEntity) = dao.upsert(device)
    override suspend fun getCapabilities(): Set<WearableCapability> =
        dao.getDevice()?.capabilitiesRaw?.let { fromCsv(it) } ?: emptySet()
    override suspend fun deleteAll() = dao.deleteAll()
}
