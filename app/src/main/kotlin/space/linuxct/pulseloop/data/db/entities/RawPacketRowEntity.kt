package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_packets")
data class RawPacketRowEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val directionRaw: String,
    val commandId: Int,
    val hexPayload: String,
    val confidenceRaw: String,
    val characteristicUuid: String,
    val deviceTypeRaw: String
)
