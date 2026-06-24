package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_daily",
    indices = [Index(value = ["date"], unique = true)]
)
data class ActivityDailyEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val steps: Int,
    val calories: Double,
    val distanceMeters: Double,
    val activeMinutes: Int,
    val source: String,
    val updatedAt: Long
)
