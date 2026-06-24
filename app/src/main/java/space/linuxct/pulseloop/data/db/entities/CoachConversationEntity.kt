package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coach_conversations")
data class CoachConversationEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastResponseId: String?,
    val totalRounds: Int = 0
)
