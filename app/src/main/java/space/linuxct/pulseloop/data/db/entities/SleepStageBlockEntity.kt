package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_stage_blocks",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SleepStageBlockEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val stageRaw: String,
    val startAt: Long,
    val durationMinutes: Int
)
