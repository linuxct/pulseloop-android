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
    val range: StateFlow<SleepRangeKey> = _range.asStateFlow()

    val uiState: StateFlow<SleepUiState> = combine(
        sleepRepo.observeSessions(),
        profileRepo.observeGoals(),
        _range
    ) { sessions, goals, range ->
        val blocks = sessions.flatMap { s -> sleepRepo.getBlocksForSession(s.id) }
        val blocksMap = blocks.groupBy { it.sessionId }
        val rangeSummary = SleepService.sleepRange(range, sessions, blocksMap)
        val goalMin = goals?.sleepMinutes

        val bars = if (range == SleepRangeKey.YEAR) {
            SleepInsights.buildMonthBuckets(rangeSummary.end, rangeSummary.sessions)
        } else if (range != SleepRangeKey.DAY) {
            SleepInsights.buildNightAxis(rangeSummary.start, rangeSummary.end, rangeSummary.sessions, range)
        } else emptyList()

        SleepUiState(
            range = range,
            rangeSummary = rangeSummary,
            goalMinutes = goalMin,
            bars = bars
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    fun setRange(range: SleepRangeKey) {
        _range.value = range
    }
}
