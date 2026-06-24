package space.linuxct.pulseloop.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity

@Dao
interface ProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 'default' LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 'default' LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_goals WHERE id = 'default' LIMIT 1")
    suspend fun getGoals(): UserGoalEntity?

    @Query("SELECT * FROM user_goals WHERE id = 'default' LIMIT 1")
    fun observeGoals(): Flow<UserGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: UserGoalEntity)
}
