package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import space.linuxct.pulseloop.ble.RingBLEClient
import javax.inject.Inject

@HiltViewModel
class HeaderViewModel @Inject constructor(bleClient: RingBLEClient) : ViewModel() {
    val connectionState = bleClient.connectionState
    val batteryPercent  = bleClient.batteryPercent
}
