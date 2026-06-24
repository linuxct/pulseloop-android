package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_sessions")
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val activityType: String,
    val statusRaw: String,
    val startedAt: Long,
    val pausedAt: Long?,
    val finishedAt: Long?,
    val useGps: Boolean,
    val totalSteps: Int,
    val totalCalories: Double,
    val totalDistanceMeters: Double,
    val totalActiveMinutes: Int,
    val hrPollCount: Int,
    val hrPollSuccessCount: Int,
    val spo2PollCount: Int,
    val spo2PollSuccessCount: Int,
    val elapsedPausedMs: Long,
    val notes: String?
)
