package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_samples",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("measurementId")]
)
data class ActivitySampleEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val measurementId: String?,
    val kindRaw: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val sourceRaw: String
)
