package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.domain.model.WearableCapability

interface DeviceRepository {
    fun observeDevice(): Flow<DeviceEntity?>
    suspend fun getDevice(): DeviceEntity?
    suspend fun upsert(device: DeviceEntity)
    suspend fun getCapabilities(): Set<WearableCapability>
    suspend fun deleteAll()
}
