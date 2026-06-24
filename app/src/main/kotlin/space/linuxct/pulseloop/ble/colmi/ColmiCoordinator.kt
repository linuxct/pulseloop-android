package space.linuxct.pulseloop.ble.colmi

import space.linuxct.pulseloop.ble.AdvertisementInfo
import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.WearableCoordinator
import space.linuxct.pulseloop.ble.WearableDriver
import space.linuxct.pulseloop.domain.model.RingDeviceType
import space.linuxct.pulseloop.domain.model.WearableCapability

class ColmiCoordinator : WearableCoordinator {

    override val deviceType: RingDeviceType = RingDeviceType.COLMI

    override val capabilities: Set<WearableCapability> = setOf(
        WearableCapability.HEART_RATE,
        WearableCapability.SPO2,
        WearableCapability.STEPS,
        WearableCapability.SLEEP,
        WearableCapability.BATTERY,
        WearableCapability.REM_SLEEP,
        WearableCapability.STRESS,
        WearableCapability.HRV,
        WearableCapability.TEMPERATURE,
        WearableCapability.MANUAL_HEART_RATE,
        WearableCapability.REALTIME_HEART_RATE,
        WearableCapability.REALTIME_STEPS,
        WearableCapability.FIND_DEVICE,
        WearableCapability.POWER_OFF,
        WearableCapability.FACTORY_RESET
    )

    override fun matches(name: String?, advertisement: AdvertisementInfo): Boolean {
        if (name != null && NAME_REGEX.containsMatchIn(name)) return true
        return advertisement.serviceUUIDs.any { it == ColmiUUIDs.SERVICE_V1 || it == ColmiUUIDs.SERVICE_V2 }
    }

    override fun makeDriver(writer: RingCommandWriter): WearableDriver = ColmiDriver(writer)

    private companion object {
        val NAME_REGEX = Regex(
            "^R0[236]_.*|^R05_[0-9A-F]{4}$|^R09_.*|^R10_[0-9A-F]{4}$|" +
            "^R11C?_[0-9A-F]{4}$|^COLMI R07_.*|^COLMI R10_.*|^COLMI R12_.*|^H59_.*"
        )
    }
}
