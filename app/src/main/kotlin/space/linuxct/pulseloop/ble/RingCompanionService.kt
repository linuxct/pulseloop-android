package space.linuxct.pulseloop.ble

import android.companion.CompanionDeviceManager
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RingCompanionService : CompanionDeviceService() {

    @Inject lateinit var ringBLEClient: RingBLEClient

    @android.annotation.SuppressLint("NewApi")
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        when (event.event) {
            DevicePresenceEvent.EVENT_BLE_APPEARED -> {
                val cdm = getSystemService(CompanionDeviceManager::class.java) ?: return
                val info = cdm.myAssociations.firstOrNull { it.id == event.associationId } ?: return
                val address = info.deviceMacAddress?.toString() ?: return
                ringBLEClient.connectToAddress(address)
            }
            DevicePresenceEvent.EVENT_BLE_DISAPPEARED -> {
                ringBLEClient.onCompanionDeviceDisappeared()
            }
        }
    }
}
