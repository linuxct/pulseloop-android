package space.linuxct.pulseloop.ble.jring

import java.util.UUID

object JringUUIDs {
    val SERVICE    = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb")
    val WRITE      = UUID.fromString("000033f3-0000-1000-8000-00805f9b34fb")
    val NOTIFY     = UUID.fromString("000033f4-0000-1000-8000-00805f9b34fb")
    val BATTERY_SVC = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY    = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
}
