package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wearable_logs")
data class WearableLogEntity(
    @PrimaryKey val id: String,
    val at: Long,
    val category: String,
    val level: String,
    val message: String,
    val detail: String?
)
