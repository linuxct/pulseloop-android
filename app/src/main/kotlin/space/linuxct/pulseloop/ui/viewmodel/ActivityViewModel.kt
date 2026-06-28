package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.ActivityBar
import space.linuxct.pulseloop.domain.model.ActivityRangeKey
import space.linuxct.pulseloop.domain.model.GoalsSummary
import space.linuxct.pulseloop.domain.model.DailyMetricPoint
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.service.MetricsService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ActivityUiState(
    val sessions: List<ActivitySessionEntity> = emptyList(),
    val activityRows: List<ActivityDailyEntity> = emptyList(),
    val goals: GoalsSummary = GoalsSummary(8000, 60, 7.5, 4),
    val steps7d: List<DailyMetricPoint> = emptyList(),
    val todaySteps: Int? = null,
    val todayCalories: Double? = null,
    val todayDistanceMeters: Double? = null,
    val todayActiveMinutes: Int? = null,
    val activityRange: ActivityRangeKey = ActivityRangeKey.DAY,
    val activityBars: List<ActivityBar> = emptyList(),
    val stepsRangeTotal: Int? = null,
    val caloriesRangeTotal: Double? = null,
    val distanceRangeTotalM: Double? = null,
    val activeMinutesRangeTotal: Int? = null,
    val selectedDate: Long? = null,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepo: ActivityRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _activityRange = MutableStateFlow(ActivityRangeKey.DAY)
    private val _selectedDate = MutableStateFlow<Long?>(null)
    fun setActivityRange(range: ActivityRangeKey) { _activityRange.value = range }
    fun setSelectedDate(dateMs: Long?) { _selectedDate.value = dateMs }

    val uiState: StateFlow<ActivityUiState> = combine(
        activityRepo.observeSessions(),
        activityRepo.observeAll(),
        profileRepo.observeGoals(),
        combine(profileRepo.observeProfile(), _activityRange, _selectedDate) { p, r, d -> Triple(p, r, d) }
    ) { sessions, rows, goals, (profile, range, selectedDate) ->
        val goalsSummary = goals?.let {
            GoalsSummary(stepsDaily = it.dailySteps, activeMinutesDaily = it.activeMinutes, sleepHours = it.sleepMinutes / 60.0, exerciseDaysWeekly = 4)
        } ?: GoalsSummary(8000, 60, 7.5, 4)

        val refMidnight = selectedDate ?: todayMidnightMs()
        val today = rows.firstOrNull { it.date in refMidnight until (refMidnight + 86_400_000L) }

        val sevenDaysAgo = refMidnight - 7L * 86_400_000
        val week = rows.filter { it.date >= sevenDaysAgo && it.date <= refMidnight }.sortedBy { it.date }
        val steps7d = week.map { DailyMetricPoint(it.date, it.steps.toDouble()) }

        val bars = buildActivityBars(range, rows)
        val presentBars = bars.filter { it.present }
        val stepsTotal = presentBars.mapNotNull { it.steps }.takeIf { it.isNotEmpty() }?.sum()
        val calTotal   = presentBars.mapNotNull { it.calories }.takeIf { it.isNotEmpty() }?.sum()
        val distTotal  = presentBars.mapNotNull { it.distanceMeters }.takeIf { it.isNotEmpty() }?.sum()
        val activeTotal = presentBars.mapNotNull { it.activeMinutes }.takeIf { it.isNotEmpty() }?.sum()

        ActivityUiState(
            sessions = sessions.sortedByDescending { it.startedAt },
            activityRows = rows,
            goals = goalsSummary,
            steps7d = steps7d,
            todaySteps = today?.steps,
            todayCalories = MetricsService.computeCalories(today, profile),
            todayDistanceMeters = today?.distanceMeters,
            todayActiveMinutes = today?.activeMinutes,
            activityRange = range,
            activityBars = bars,
            stepsRangeTotal = stepsTotal,
            caloriesRangeTotal = calTotal,
            distanceRangeTotalM = distTotal,
            activeMinutesRangeTotal = activeTotal,
            selectedDate = selectedDate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

private fun buildActivityBars(
    range: ActivityRangeKey,
    rows: List<space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity>
): List<ActivityBar> {
    if (range == ActivityRangeKey.DAY) return emptyList()
    if (range == ActivityRangeKey.YEAR) {
        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
        return (11 downTo 0).map { i ->
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -i)
            }
            val monthStart = monthCal.timeInMillis
            monthCal.add(Calendar.MONTH, 1)
            val monthEnd = monthCal.timeInMillis
            val monthRows = rows.filter { it.date in monthStart until monthEnd }
            val active = monthRows.any { it.steps > 0 || it.distanceMeters > 0.0 || it.activeMinutes > 0 }
            ActivityBar(
                label          = monthFmt.format(Date(monthStart)),
                steps          = monthRows.sumOf { it.steps }.takeIf { monthRows.isNotEmpty() },
                calories       = monthRows.mapNotNull { MetricsService.computeCalories(it, null) }.let { if (it.isEmpty()) null else it.sum() },
                distanceMeters = monthRows.sumOf { it.distanceMeters }.takeIf { monthRows.isNotEmpty() },
                activeMinutes  = monthRows.sumOf { it.activeMinutes }.takeIf { monthRows.isNotEmpty() },
                present        = active
            )
        }
    }
    val dayCount = if (range == ActivityRangeKey.WEEK) 7 else 30
    val todayMidnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayFmt = Calendar.getInstance()
    return (dayCount - 1 downTo 0).map { i ->
        val dayStart = todayMidnight - i * 86_400_000L
        val dayEnd   = dayStart + 86_400_000L
        val row = rows.firstOrNull { it.date in dayStart until dayEnd }
        dayFmt.timeInMillis = dayStart
        val label = if (range == ActivityRangeKey.WEEK)
            listOf("Su","Mo","Tu","We","Th","Fr","Sa")[dayFmt.get(Calendar.DAY_OF_WEEK) - 1]
        else
            "${dayFmt.get(Calendar.DAY_OF_MONTH)}"
        val active = row != null && ((row.steps > 0) || (row.distanceMeters > 0.0) || (row.activeMinutes > 0))
        ActivityBar(
            label          = label,
            steps          = row?.steps,
            calories       = row?.let { MetricsService.computeCalories(it, null) },
            distanceMeters = row?.distanceMeters,
            activeMinutes  = row?.activeMinutes,
            present        = active
        )
    }
}
