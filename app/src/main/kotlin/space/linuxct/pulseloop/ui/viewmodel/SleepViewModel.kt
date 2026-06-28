package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import space.linuxct.pulseloop.domain.model.SleepBar
import space.linuxct.pulseloop.domain.model.SleepRangeKey
import space.linuxct.pulseloop.domain.model.SleepRangeSummary
import space.linuxct.pulseloop.domain.model.SleepSummary
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.repository.SleepRepository
import space.linuxct.pulseloop.domain.service.SleepInsights
import space.linuxct.pulseloop.domain.service.SleepService
import javax.inject.Inject

data class SleepUiState(
    val range: SleepRangeKey = SleepRangeKey.DAY,
    val selectedDate: Long? = null,
    val rangeSummary: SleepRangeSummary? = null,
    val goalMinutes: Int? = null,
    val bars: List<SleepBar> = emptyList()
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val sleepRepo: SleepRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _range = MutableStateFlow(SleepRangeKey.DAY)
    private val _selectedDate = MutableStateFlow<Long?>(null)
    val range: StateFlow<SleepRangeKey> = _range.asStateFlow()

    val uiState: StateFlow<SleepUiState> = combine(
        sleepRepo.observeSessions(),
        profileRepo.observeGoals(),
        combine(_range, _selectedDate) { r, d -> Pair(r, d) }
    ) { sessions, goals, (range, selectedDate) ->
        val blocks = sessions.flatMap { s -> sleepRepo.getBlocksForSession(s.id) }
        val blocksMap = blocks.groupBy { it.sessionId }
        val rangeSummary = if (selectedDate != null && range == SleepRangeKey.DAY) {
            val anchor = SleepService.dayReferenceNight(selectedDate + 12L * 3_600_000L)
            val startMs = anchor
            val endMs = anchor + 86_400_000L - 1L
            val summaries = sessions.filter { it.startAt in startMs..endMs }.map { session ->
                SleepService.summary(session, blocksMap[session.id] ?: emptyList(), includeStages = true)
            }
            SleepRangeSummary(range = SleepRangeKey.DAY, start = startMs, end = anchor, expectedNights = 1, sessions = summaries)
        } else {
            SleepService.sleepRange(range, sessions, blocksMap)
        }
        val goalMin = goals?.sleepMinutes

        val bars = if (range == SleepRangeKey.YEAR) {
            SleepInsights.buildMonthBuckets(rangeSummary.end, rangeSummary.sessions)
        } else if (range != SleepRangeKey.DAY) {
            SleepInsights.buildNightAxis(rangeSummary.start, rangeSummary.end, rangeSummary.sessions, range)
        } else emptyList()

        SleepUiState(
            range = range,
            selectedDate = selectedDate,
            rangeSummary = rangeSummary,
            goalMinutes = goalMin,
            bars = bars
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    fun setRange(range: SleepRangeKey) { _range.value = range }
    fun setSelectedDate(dateMs: Long?) { _selectedDate.value = dateMs }
}
