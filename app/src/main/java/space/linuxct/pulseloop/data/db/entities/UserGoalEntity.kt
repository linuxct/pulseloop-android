package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoalEntity(
    @PrimaryKey val id: String = "default",
    val dailySteps: Int = 10_000,
    val sleepMinutes: Int = 480,
    val activeMinutes: Int = 45,
    val updatedAt: Long
)
