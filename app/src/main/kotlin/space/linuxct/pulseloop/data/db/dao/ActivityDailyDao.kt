package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity

@Dao
interface ActivityDailyDao {

    @Query("SELECT * FROM activity_daily ORDER BY date DESC")
    fun observeAll(): Flow<List<ActivityDailyEntity>>

    @Query("SELECT * FROM activity_daily ORDER BY date DESC")
    suspend fun getAll(): List<ActivityDailyEntity>

    @Query("SELECT * FROM activity_daily WHERE date = :midnightMs LIMIT 1")
    suspend fun getForDate(midnightMs: Long): ActivityDailyEntity?

    @Query("SELECT * FROM activity_daily WHERE date >= :cutoffMs ORDER BY date DESC")
    suspend fun getSince(cutoffMs: Long): List<ActivityDailyEntity>

    @Query("SELECT * FROM activity_daily WHERE date >= :startMs AND date <= :endMs ORDER BY date DESC")
    suspend fun getBetween(startMs: Long, endMs: Long): List<ActivityDailyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ActivityDailyEntity)
}
