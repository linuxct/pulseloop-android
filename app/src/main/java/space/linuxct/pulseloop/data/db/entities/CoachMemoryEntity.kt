package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coach_memories",
    indices = [Index(value = ["key"], unique = true)]
)
data class CoachMemoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val value: String,
    val importance: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long?
)
