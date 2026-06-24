package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.GoalsSummary
import space.linuxct.pulseloop.domain.model.DailyMetricPoint
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.service.MetricsService
import javax.inject.Inject

data class ActivityUiState(
    val sessions: List<ActivitySessionEntity> = emptyList(),
    val activityRows: List<ActivityDailyEntity> = emptyList(),
    val goals: GoalsSummary = GoalsSummary(8000, 60, 7.5, 4),
    val steps7d: List<DailyMetricPoint> = emptyList(),
    val todaySteps: Int? = null,
    val todayCalories: Double? = null,
    val todayDistanceMeters: Double? = null,
    val todayActiveMinutes: Int? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepo: ActivityRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    val uiState: StateFlow<ActivityUiState> = combine(
        activityRepo.observeSessions(),
        activityRepo.observeAll(),
        profileRepo.observeGoals(),
        profileRepo.observeProfile()
    ) { sessions, rows, goals, profile ->
        val goalsSummary = goals?.let {
            GoalsSummary(stepsDaily = it.dailySteps, activeMinutesDaily = it.activeMinutes, sleepHours = it.sleepMinutes / 60.0, exerciseDaysWeekly = 4)
        } ?: GoalsSummary(8000, 60, 7.5, 4)

        val todayMidnight = todayMidnightMs()
        val today = rows.filter { it.date <= todayMidnight }.maxByOrNull { it.date }

        val sevenDaysAgo = todayMidnight - 7L * 86_400_000
        val week = rows.filter { it.date >= sevenDaysAgo }.sortedBy { it.date }
        val steps7d = week.map { DailyMetricPoint(it.date, it.steps.toDouble()) }

        ActivityUiState(
            sessions = sessions.sortedByDescending { it.startedAt },
            activityRows = rows,
            goals = goalsSummary,
            steps7d = steps7d,
            todaySteps = today?.steps,
            todayCalories = MetricsService.computeCalories(today, profile),
            todayDistanceMeters = today?.distanceMeters,
            todayActiveMinutes = today?.activeMinutes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    private fun todayMidnightMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
