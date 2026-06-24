package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_sensor_polls",
    foreignKeys = [
        ForeignKey(
            entity = ActivitySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ActivitySensorPollEventEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestamp: Long,
    val sensorKindRaw: String,
    val statusRaw: String,
    val resultValue: Double?,
    val errorMessage: String?
)
