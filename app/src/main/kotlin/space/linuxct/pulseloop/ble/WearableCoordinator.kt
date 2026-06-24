package space.linuxct.pulseloop.ble

import space.linuxct.pulseloop.domain.model.RingDeviceType
import space.linuxct.pulseloop.domain.model.WearableCapability
import java.util.UUID

data class AdvertisementInfo(
    val serviceUUIDs: List<UUID>,
    val manufacturerData: ByteArray?
)

interface WearableCoordinator {
    val deviceType: RingDeviceType
    val capabilities: Set<WearableCapability>

    fun matches(name: String?, advertisement: AdvertisementInfo): Boolean

    fun makeDriver(writer: RingCommandWriter): WearableDriver
}
