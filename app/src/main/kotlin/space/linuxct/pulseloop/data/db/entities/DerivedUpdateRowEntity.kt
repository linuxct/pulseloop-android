package space.linuxct.pulseloop.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "derived_updates")
data class DerivedUpdateRowEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val eventType: String,
    val detail: String?
)
