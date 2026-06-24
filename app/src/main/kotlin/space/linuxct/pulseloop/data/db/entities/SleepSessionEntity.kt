package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val startAt: Long,
    val endAt: Long,
    val score: Int?,
    val syncedAt: Long?,
    val deviceId: String?
)
