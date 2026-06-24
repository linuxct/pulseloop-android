package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.service.ActivityRecorderService
import javax.inject.Inject

data class ActivityDetailUiState(
    val session: ActivitySessionEntity? = null,
    val hrSamples: List<MetricSample> = emptyList(),
    val spo2Samples: List<MetricSample> = emptyList(),
    val gpsPoints: List<ActivityGpsPointEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepo: ActivityRepository,
    private val recorderService: ActivityRecorderService
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val session = activityRepo.getSessionById(sessionId)
        val samples = activityRepo.getSamplesForSession(sessionId)
        val gpsPoints = activityRepo.getGpsPointsForSession(sessionId).filter { it.accepted }
        val hrSamples = samples
            .filter { it.kindRaw == "hr" && it.value > 0 }
            .sortedBy { it.timestamp }
            .map { MetricSample(it.timestamp, it.value) }
        val spo2Samples = samples
            .filter { it.kindRaw == "spo2" && it.value > 0 }
            .sortedBy { it.timestamp }
            .map { MetricSample(it.timestamp, it.value) }
        _uiState.value = ActivityDetailUiState(
            session = session,
            hrSamples = hrSamples,
            spo2Samples = spo2Samples,
            gpsPoints = gpsPoints,
            isLoading = false
        )
    }

    fun deleteSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            recorderService.delete(session)
            onDeleted()
        }
    }
}
