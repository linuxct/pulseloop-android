package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_summaries",
    indices = [Index(value = ["kind", "scopeKey"], unique = true)]
)
data class CoachSummaryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val scopeKey: String,
    val contentJson: String,
    val dataSignature: String,
    val generatedAt: Long,
    val modelUsed: String
)
