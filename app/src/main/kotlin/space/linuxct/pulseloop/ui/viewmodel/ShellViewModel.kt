package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.data.db.entities.UserGoalEntity
import space.linuxct.pulseloop.data.db.entities.UserProfileEntity
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.workout.LiveWorkoutManager
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val activityRepo: ActivityRepository,
    private val liveWorkoutManager: LiveWorkoutManager
) : ViewModel() {

    // null = DB not yet queried; false = no profile or not completed; true = done
    val onboardingCompleted: StateFlow<Boolean?> = profileRepo.observeProfile()
        .map { it?.onboardingCompleted ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pendingWorkoutDeepLink: StateFlow<String?> = liveWorkoutManager.pendingDeepLinkSession

    fun clearDeepLink() = liveWorkoutManager.clearDeepLink()

    fun isFinished(sessionId: String): Boolean = false // will be resolved asynchronously

    fun saveProfile(name: String?, age: Int?, biologicalSex: String?, weightKg: Double?, heightCm: Int?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = profileRepo.getProfile()
            val updated = existing?.copy(
                name = name, age = age, biologicalSex = biologicalSex,
                weightKg = weightKg, heightCm = heightCm, updatedAt = now
            ) ?: UserProfileEntity(
                name = name, age = age, heightCm = heightCm, weightKg = weightKg,
                biologicalSex = biologicalSex, onboardingCompleted = false,
                createdAt = now, updatedAt = now
            )
            profileRepo.upsertProfile(updated)
        }
    }

    fun saveGoals(steps: Int, sleepMinutes: Int, activeMinutes: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = profileRepo.getGoals()
            if (existing != null) {
                profileRepo.upsertGoals(existing.copy(dailySteps = steps, sleepMinutes = sleepMinutes, activeMinutes = activeMinutes, updatedAt = now))
            } else {
                profileRepo.upsertGoals(UserGoalEntity(dailySteps = steps, sleepMinutes = sleepMinutes, activeMinutes = activeMinutes, updatedAt = now))
            }
        }
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = profileRepo.getProfile()
            val updated = existing?.copy(onboardingCompleted = true)
                ?: UserProfileEntity(
                    name = null, age = null, heightCm = null, weightKg = null,
                    biologicalSex = null, onboardingCompleted = true,
                    createdAt = now, updatedAt = now
                )
            profileRepo.upsertProfile(updated)
        }
    }
}
