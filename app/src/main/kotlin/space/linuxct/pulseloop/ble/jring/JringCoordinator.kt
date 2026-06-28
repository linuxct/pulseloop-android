package space.linuxct.pulseloop.ble.jring

import space.linuxct.pulseloop.ble.AdvertisementInfo
import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.WearableCoordinator
import space.linuxct.pulseloop.ble.WearableDriver
import space.linuxct.pulseloop.core.util.toHexString
import space.linuxct.pulseloop.domain.model.RingDeviceType
import space.linuxct.pulseloop.domain.model.WearableCapability

class JringCoordinator : WearableCoordinator {

    override val deviceType: RingDeviceType = RingDeviceType.JRING

    override val capabilities: Set<WearableCapability> = setOf(
        WearableCapability.HEART_RATE,
        WearableCapability.SPO2,
        WearableCapability.STEPS,
        WearableCapability.SLEEP,
        WearableCapability.BATTERY,
        WearableCapability.MANUAL_HEART_RATE,
        WearableCapability.MANUAL_SPO2,
        WearableCapability.REALTIME_HEART_RATE,
        WearableCapability.FIND_DEVICE,
        // Combined-measurement metrics (0x23/0x24). Jring exposes BP + blood sugar (with calibration),
        // plus stress and fatigue. It has no skin-temperature sensor and no reliable HRV.
        WearableCapability.BLOOD_PRESSURE,
        WearableCapability.BLOOD_SUGAR,
        WearableCapability.STRESS,
        WearableCapability.FATIGUE
    )

    override fun matches(name: String?, advertisement: AdvertisementInfo): Boolean {
        if (name == ADVERTISED_NAME) return true
        if (advertisement.serviceUUIDs.contains(JringUUIDs.SERVICE)) return true
        val mfgHex = advertisement.manufacturerData?.toHexString() ?: return false
        return mfgHex.contains(MANUFACTURER_HEX_NEEDLE)
    }

    override fun makeDriver(writer: RingCommandWriter): WearableDriver = JringDriver()

    companion object {
        private const val ADVERTISED_NAME        = "SMART_RING"
        private const val MANUFACTURER_HEX_NEEDLE = "41422ec75b6a"
    }
}
