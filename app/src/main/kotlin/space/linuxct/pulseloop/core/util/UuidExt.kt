package space.linuxct.pulseloop.core.util

import java.util.UUID

fun uuidFromShort(short: String): java.util.UUID =
    UUID.fromString("0000${short}-0000-1000-8000-00805f9b34fb")

fun String.toParcelUuid(): android.os.ParcelUuid =
    android.os.ParcelUuid.fromString(this)
