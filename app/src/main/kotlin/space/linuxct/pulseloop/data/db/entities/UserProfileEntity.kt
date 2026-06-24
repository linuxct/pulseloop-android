package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default",
    val name: String?,
    val age: Int?,
    val heightCm: Int?,
    val weightKg: Double?,
    val biologicalSex: String?,
    val onboardingCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
