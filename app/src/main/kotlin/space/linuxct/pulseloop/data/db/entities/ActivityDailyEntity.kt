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
    val updatedAt: Long,
    // Ring's cumulative reading at the start of the current counting window.
    // Resets to 0 whenever a mid-day ring hardware reset is detected.
    val stepBaseline: Int = 0,
    // Daily steps accumulated in all closed windows before the last ring reset.
    // Display value = stepsSaved + (current ring reading − stepBaseline).
    // Accumulates on each detected intra-day ring reset.
    val stepsSaved: Int = 0
)
