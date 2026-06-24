package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val macAddress: String,
    val stateRaw: String,
    val deviceTypeRaw: String,
    val capabilitiesRaw: String,
    val firmwareVersion: String?,
    val hardwareVersion: String?,
    val batteryLevel: Int?,
    val associationId: Int?,
    val lastSeenAt: Long?,
    val pairedAt: Long
)
