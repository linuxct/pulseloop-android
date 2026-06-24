package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    indices = [Index(value = ["kindRaw", "timestamp"], unique = true)]
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    val kindRaw: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val sourceRaw: String,
    val confidenceRaw: String,
    val activitySessionId: String?
)
