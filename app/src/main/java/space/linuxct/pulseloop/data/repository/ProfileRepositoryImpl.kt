package space.linuxct.pulseloop.data.repository

import kotlinx.coroutines.flow.Flow
import space.linuxct.pulseloop.data.db.dao.ProfileDao
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val dao: ProfileDao
) : ProfileRepository {
    override suspend fun getProfile() = dao.getProfile()
    override fun observeProfile(): Flow<UserProfileEntity?> = dao.observeProfile()
    override suspend fun upsertProfile(profile: UserProfileEntity) = dao.upsertProfile(profile)
    override suspend fun getGoals() = dao.getGoals()
    override fun observeGoals(): Flow<UserGoalEntity?> = dao.observeGoals()
    override suspend fun upsertGoals(goals: UserGoalEntity) = dao.upsertGoals(goals)
}
