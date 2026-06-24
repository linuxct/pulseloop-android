package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_messages",
    foreignKeys = [
        ForeignKey(
            entity = CoachConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class CoachMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val roleRaw: String,
    val textContent: String?,
    val cardsJson: String?,
    val pendingActionJson: String?,
    val timestamp: Long,
    val responseId: String?,
    val modelUsed: String?,
    val confidenceRaw: String?
)
