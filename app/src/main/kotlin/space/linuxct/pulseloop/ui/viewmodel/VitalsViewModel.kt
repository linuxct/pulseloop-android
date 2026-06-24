package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.domain.model.MetricKey
import space.linuxct.pulseloop.domain.model.MetricRange
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.WearableCapability
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.service.MetricsService
import javax.inject.Inject

data class VitalsUiState(
    val hrSamples: List<MetricSample> = emptyList(),
    val spo2Samples: List<MetricSample> = emptyList(),
    val stressSamples: List<MetricSample> = emptyList(),
    val hrvSamples: List<MetricSample> = emptyList(),
    val tempSamples: List<MetricSample> = emptyList(),
    val latestHR: Double? = null,
    val latestSpO2: Double? = null,
    val restingHREstimate: Double? = null,
    val peakHRToday: Double? = null,
    val capabilities: Set<WearableCapability> = emptySet(),
    val device: DeviceEntity? = null,
    val isMeasuringHR: Boolean = false,
    val isMeasuringSpO2: Boolean = false,
    val connectionState: RingConnectionState = RingConnectionState.IDLE
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val measurementRepo: MeasurementRepository,
    private val activityRepo: ActivityRepository,
    private val deviceRepo: DeviceRepository,
    private val bleClient: RingBLEClient
) : ViewModel() {

    private val cutoff24h = System.currentTimeMillis() - 24L * 3_600_000
    private val _isMeasuringHR   = MutableStateFlow(false)
    private val _isMeasuringSpO2 = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            PulseEventBus.events.collect { event ->
                when (event) {
                    is PulseEvent.HeartRateComplete -> {
                        if (_isMeasuringHR.value) {
                            _isMeasuringHR.value = false
                            bleClient.stopHR()
                        }
                    }
                    is PulseEvent.Spo2Complete -> _isMeasuringSpO2.value = false
                    is PulseEvent.Spo2Result   -> _isMeasuringSpO2.value = false
                    else -> Unit
                }
            }
        }
    }

    val uiState: StateFlow<VitalsUiState> = combine(
        combine(deviceRepo.observeDevice(), bleClient.connectionState) { d, c -> Pair(d, c) },
        combine(measurementRepo.observeSince(cutoff24h), activityRepo.observeAll()) { m, a -> Pair(m, a) },
        _isMeasuringHR,
        _isMeasuringSpO2
    ) { (device, connState), (allMeasurements, activityRows), isMeasuringHR, isMeasuringSpO2 ->
        val caps = MetricsService.deviceCapabilities(device)

        val hrSamples     = MetricsService.metricRange(MetricKey.HEART_RATE,  MetricRange.TWENTY_FOUR_HOURS, allMeasurements, activityRows)
        val spo2Samples   = MetricsService.metricRange(MetricKey.SPO2,        MetricRange.TWENTY_FOUR_HOURS, allMeasurements, activityRows)
        val stressSamples = MetricsService.metricRange(MetricKey.STRESS,      MetricRange.TWENTY_FOUR_HOURS, allMeasurements, activityRows)
        val hrvSamples    = MetricsService.metricRange(MetricKey.HRV,         MetricRange.TWENTY_FOUR_HOURS, allMeasurements, activityRows)
        val tempSamples   = MetricsService.metricRange(MetricKey.TEMPERATURE, MetricRange.TWENTY_FOUR_HOURS, allMeasurements, activityRows)

        val latestHR   = allMeasurements.filter { it.kindRaw == MeasurementKind.HEART_RATE.rawValue }.maxByOrNull { it.timestamp }?.value
        val latestSpO2 = allMeasurements.filter { it.kindRaw == MeasurementKind.SPO2.rawValue }.maxByOrNull { it.timestamp }?.value

        // Compute stats from the full-resolution list before chart decimation
        val restingHR = hrSamples.map { it.value }.filter { it <= 72 }.minOrNull()
        val peakHR    = hrSamples.maxOfOrNull { it.value }

        VitalsUiState(
            hrSamples = hrSamples.decimateForChart(),
            spo2Samples = spo2Samples.decimateForChart(),
            stressSamples = stressSamples.decimateForChart(),
            hrvSamples = hrvSamples.decimateForChart(),
            tempSamples = tempSamples.decimateForChart(),
            latestHR = latestHR,
            latestSpO2 = latestSpO2,
            restingHREstimate = restingHR,
            peakHRToday = peakHR,
            capabilities = caps,
            device = device,
            isMeasuringHR = isMeasuringHR,
            isMeasuringSpO2 = isMeasuringSpO2,
            connectionState = connState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VitalsUiState())

    fun startHRMeasurement() {
        if (bleClient.hrActive) return
        _isMeasuringHR.value = true
        bleClient.measureHR()
    }

    fun stopHRMeasurement() {
        _isMeasuringHR.value = false
        bleClient.stopHR()
    }

    fun startSpO2Measurement() {
        _isMeasuringSpO2.value = true
        bleClient.measureSpO2()
    }

    fun stopSpO2Measurement() {
        _isMeasuringSpO2.value = false
        bleClient.stopSpO2()
    }

}

private const val CHART_MAX_POINTS = 48
private const val CHART_BUCKET_MS  = 30 * 60 * 1_000L

private fun List<MetricSample>.decimateForChart(): List<MetricSample> {
    if (size <= CHART_MAX_POINTS) return this
    val buckets = groupBy { it.timestamp / CHART_BUCKET_MS }
        .entries
        .sortedBy { it.key }
    return buckets.mapIndexed { index, (_, group) ->
        if (index == buckets.lastIndex) {
            group.last()
        } else {
            MetricSample(
                timestamp = group.first().timestamp,
                value = group.sumOf { it.value } / group.size
            )
        }
    }
}
