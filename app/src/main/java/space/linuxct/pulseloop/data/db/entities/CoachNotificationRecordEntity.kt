package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_notification_records",
    indices = [Index(value = ["dateKey", "slotRaw"], unique = true)]
)
data class CoachNotificationRecordEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val slotRaw: String,
    val sentAt: Long,
    val conversationId: String?,
    val title: String
)
