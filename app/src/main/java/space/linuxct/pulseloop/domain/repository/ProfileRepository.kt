package space.linuxct.pulseloop.domain.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity

interface ProfileRepository {
    suspend fun getProfile(): UserProfileEntity?
    fun observeProfile(): Flow<UserProfileEntity?>
    suspend fun upsertProfile(profile: UserProfileEntity)
    suspend fun getGoals(): UserGoalEntity?
    fun observeGoals(): Flow<UserGoalEntity?>
    suspend fun upsertGoals(goals: UserGoalEntity)
}
