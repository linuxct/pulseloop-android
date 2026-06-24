package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_tool_calls",
    foreignKeys = [
        ForeignKey(
            entity = CoachMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class CoachToolCallEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val conversationId: String,
    val toolName: String,
    val callId: String,
    val inputJson: String?,
    val outputJson: String?,
    val statusRaw: String,
    val durationMs: Long?,
    val timestamp: Long
)
