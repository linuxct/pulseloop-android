package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.ble.RingBLEClient
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.MetricSample
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.VitalsBar
import space.linuxct.pulseloop.domain.model.VitalsRangeKey
import space.linuxct.pulseloop.domain.model.WearableCapability
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    // Jring combined-measurement metrics (calibration offsets already applied).
    val bpSysSamples: List<MetricSample> = emptyList(),
    val bpDiaSamples: List<MetricSample> = emptyList(),
    val glucoseSamples: List<MetricSample> = emptyList(),
    val fatigueSamples: List<MetricSample> = emptyList(),
    val latestBpSys: Double? = null,
    val latestBpDia: Double? = null,
    val latestGlucose: Double? = null,
    val latestFatigue: Double? = null,
    val latestStress: Double? = null,
    val bloodMetricsEnabled: Boolean = false,
    val latestHR: Double? = null,
    val latestSpO2: Double? = null,
    val restingHREstimate: Double? = null,
    val peakHRToday: Double? = null,
    val capabilities: Set<WearableCapability> = emptySet(),
    val device: DeviceEntity? = null,
    val isMeasuringHR: Boolean = false,
    val isMeasuringSpO2: Boolean = false,
    val connectionState: RingConnectionState = RingConnectionState.IDLE,
    val vitalsRange: VitalsRangeKey = VitalsRangeKey.DAY,
    val selectedDate: Long? = null,
    val hrBars: List<VitalsBar> = emptyList(),
    val spo2Bars: List<VitalsBar> = emptyList(),
    val hrRangeAvg: Double? = null,
    val hrRangeMin: Double? = null,
    val hrRangeMax: Double? = null,
    val spo2RangeAvg: Double? = null,
    val spo2RangeMin: Double? = null,
    val spo2RangeMax: Double? = null,
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val measurementRepo: MeasurementRepository,
    private val activityRepo: ActivityRepository,
    private val deviceRepo: DeviceRepository,
    private val prefs: AppPreferencesDataStore,
    private val bleClient: RingBLEClient
) : ViewModel() {

    private val cutoff1year = System.currentTimeMillis() - 365L * 24L * 3_600_000L
    private val _isMeasuringHR   = MutableStateFlow(false)
    private val _isMeasuringSpO2 = MutableStateFlow(false)
    private val _vitalsRange = MutableStateFlow(VitalsRangeKey.DAY)
    private val _selectedDate = MutableStateFlow<Long?>(null)
    private var spO2TimeoutJob: Job? = null

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
                    is PulseEvent.Spo2Result -> {
                        spO2TimeoutJob?.cancel()
                        _isMeasuringSpO2.value = false
                        bleClient.stopSpO2()
                    }
                    is PulseEvent.Spo2Complete -> {
                        spO2TimeoutJob?.cancel()
                        _isMeasuringSpO2.value = false
                        bleClient.stopSpO2()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun setVitalsRange(range: VitalsRangeKey) { _vitalsRange.value = range }
    fun setSelectedDate(dateMs: Long?) { _selectedDate.value = dateMs }

    val uiState: StateFlow<VitalsUiState> = combine(
        combine(deviceRepo.observeDevice(), bleClient.connectionState) { d, c -> Pair(d, c) },
        combine(measurementRepo.observeSince(cutoff1year), activityRepo.observeAll()) { m, a -> Pair(m, a) },
        combine(_vitalsRange, _selectedDate) { r, d -> Pair(r, d) },
        combine(_isMeasuringHR, _isMeasuringSpO2) { hr, spo2 -> Pair(hr, spo2) },
        combine(prefs.bpCalSystolic, prefs.bpCalDiastolic, prefs.glucoseOffsetMgdl, prefs.bloodMetricsEnabled) { s, d, g, e -> VitalsCal(s, d, g, e) }
    ) { (device, connState), (allMeasurements, activityRows), (range, selectedDate), (isMeasuringHR, isMeasuringSpO2), (bpCalSys, bpCalDia, glucoseOffset, bloodEnabled) ->
        val caps = MetricsService.deviceCapabilities(device)

        val dayEnd = selectedDate?.let { it + 86_400_000L - 1L } ?: System.currentTimeMillis()
        val dayStart = selectedDate ?: (dayEnd - 86_400_000L)

        fun samplesInDay(kindRaw: String): List<MetricSample> =
            allMeasurements.filter { it.kindRaw == kindRaw && it.timestamp in dayStart..dayEnd }
                .sortedBy { it.timestamp }
                .map { MetricSample(it.timestamp, it.value) }

        val hrSamples     = samplesInDay(MeasurementKind.HEART_RATE.rawValue)
        val spo2Samples   = samplesInDay(MeasurementKind.SPO2.rawValue)
        val stressSamples = samplesInDay(MeasurementKind.STRESS.rawValue)
        val hrvSamples    = samplesInDay(MeasurementKind.HRV.rawValue)
        val tempSamples   = samplesInDay(MeasurementKind.TEMPERATURE.rawValue)
        // Blood pressure / glucose calibration is applied at display time (readings are stored raw).
        fun List<MetricSample>.offsetBy(delta: Double): List<MetricSample> =
            if (delta == 0.0) this else map { it.copy(value = it.value + delta) }
        val bpSysSamples  = samplesInDay(MeasurementKind.BLOOD_PRESSURE_SYSTOLIC.rawValue).offsetBy(bpCalSys.toDouble())
        val bpDiaSamples  = samplesInDay(MeasurementKind.BLOOD_PRESSURE_DIASTOLIC.rawValue).offsetBy(bpCalDia.toDouble())
        val glucoseSamples = samplesInDay(MeasurementKind.BLOOD_SUGAR.rawValue).offsetBy(glucoseOffset)
        val fatigueSamples = samplesInDay(MeasurementKind.FATIGUE.rawValue)

        val latestHR   = hrSamples.lastOrNull()?.value
        val latestSpO2 = spo2Samples.lastOrNull()?.value
        val restingHR  = hrSamples.map { it.value }.filter { it <= 72 }.minOrNull()
        val peakHR     = hrSamples.maxOfOrNull { it.value }

        val hrBars   = buildVitalsBars(range, MeasurementKind.HEART_RATE.rawValue, allMeasurements)
        val spo2Bars = buildVitalsBars(range, MeasurementKind.SPO2.rawValue,        allMeasurements)
        val hrPresentBars   = hrBars.filter   { it.present }
        val spo2PresentBars = spo2Bars.filter { it.present }

        VitalsUiState(
            hrSamples = hrSamples.decimateForChart(),
            spo2Samples = spo2Samples.decimateForChart(),
            stressSamples = stressSamples.decimateForChart(),
            hrvSamples = hrvSamples.decimateForChart(),
            tempSamples = tempSamples.decimateForChart(),
            bpSysSamples = bpSysSamples.decimateForChart(),
            bpDiaSamples = bpDiaSamples.decimateForChart(),
            glucoseSamples = glucoseSamples.decimateForChart(),
            fatigueSamples = fatigueSamples.decimateForChart(),
            latestBpSys = bpSysSamples.lastOrNull()?.value,
            latestBpDia = bpDiaSamples.lastOrNull()?.value,
            latestGlucose = glucoseSamples.lastOrNull()?.value,
            latestFatigue = fatigueSamples.lastOrNull()?.value,
            latestStress = stressSamples.lastOrNull()?.value,
            bloodMetricsEnabled = bloodEnabled,
            latestHR = latestHR,
            latestSpO2 = latestSpO2,
            restingHREstimate = restingHR,
            peakHRToday = peakHR,
            capabilities = caps,
            device = device,
            isMeasuringHR = isMeasuringHR,
            isMeasuringSpO2 = isMeasuringSpO2,
            connectionState = connState,
            vitalsRange = range,
            selectedDate = selectedDate,
            hrBars  = hrBars,
            spo2Bars = spo2Bars,
            hrRangeAvg  = hrPresentBars.mapNotNull  { it.avgValue }.let  { if (it.isEmpty()) null else it.average() },
            hrRangeMin  = hrPresentBars.mapNotNull  { it.minValue }.minOrNull(),
            hrRangeMax  = hrPresentBars.mapNotNull  { it.maxValue }.maxOrNull(),
            spo2RangeAvg = spo2PresentBars.mapNotNull { it.avgValue }.let { if (it.isEmpty()) null else it.average() },
            spo2RangeMin = spo2PresentBars.mapNotNull { it.minValue }.minOrNull(),
            spo2RangeMax = spo2PresentBars.mapNotNull { it.maxValue }.maxOrNull(),
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
        // If auto-trigger already has an in-progress measurement, share its result rather
        // than silently dropping the user's tap. Only send the BLE start command if the
        // ring is currently idle.
        _isMeasuringSpO2.value = true
        if (!bleClient.spO2Active) bleClient.measureSpO2()

        // Safety net: if neither Spo2Result nor Spo2Complete arrives within 45 s, reset.
        spO2TimeoutJob?.cancel()
        spO2TimeoutJob = viewModelScope.launch {
            delay(45_000L)
            if (_isMeasuringSpO2.value) {
                _isMeasuringSpO2.value = false
                bleClient.stopSpO2()
            }
        }
    }

    fun stopSpO2Measurement() {
        _isMeasuringSpO2.value = false
        bleClient.stopSpO2()
    }

    /**
     * Trigger a combined spot measurement (0x23) — one read populates BP, SpO2, stress, fatigue and
     * blood sugar. Jring exposes only this single command, so each new metric card's "Measure" button
     * calls this. Reuses the SpO2 in-progress flag/timeout because 0x23 is the same command that
     * drives the SpO2 result, and the 0x24 response completes via Spo2Result/Spo2Complete.
     */
    fun startCombinedMeasurement() {
        _isMeasuringSpO2.value = true
        if (!bleClient.spO2Active) bleClient.measureCombined()
        spO2TimeoutJob?.cancel()
        spO2TimeoutJob = viewModelScope.launch {
            delay(45_000L)
            if (_isMeasuringSpO2.value) {
                _isMeasuringSpO2.value = false
                bleClient.stopCombined()
            }
        }
    }

}

// Bundles the BP/glucose calibration + blood-metrics opt-in into one combine source.
private data class VitalsCal(
    val bpCalSystolic: Int,
    val bpCalDiastolic: Int,
    val glucoseOffset: Double,
    val bloodMetricsEnabled: Boolean,
)

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

private fun buildVitalsBars(
    range: VitalsRangeKey,
    kindRaw: String,
    measurements: List<space.linuxct.pulseloop.data.db.entities.MeasurementEntity>
): List<VitalsBar> {
    if (range == VitalsRangeKey.DAY) return emptyList()
    val filtered = measurements.filter { it.kindRaw == kindRaw }
    if (range == VitalsRangeKey.YEAR) {
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
            val slice = filtered.filter { it.timestamp in monthStart until monthEnd }
            VitalsBar(
                label    = monthFmt.format(Date(monthStart)),
                avgValue = if (slice.isEmpty()) null else slice.sumOf { it.value } / slice.size,
                minValue = slice.minOfOrNull { it.value },
                maxValue = slice.maxOfOrNull { it.value },
                present  = slice.isNotEmpty()
            )
        }
    }
    val dayCount = if (range == VitalsRangeKey.WEEK) 7 else 30
    val todayMidnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayFmt = Calendar.getInstance()
    return (dayCount - 1 downTo 0).map { i ->
        val dayStart = todayMidnight - i * 86_400_000L
        val dayEnd   = dayStart + 86_400_000L
        val slice    = filtered.filter { it.timestamp in dayStart until dayEnd }
        dayFmt.timeInMillis = dayStart
        val label = if (range == VitalsRangeKey.WEEK)
            listOf("Su","Mo","Tu","We","Th","Fr","Sa")[dayFmt.get(Calendar.DAY_OF_WEEK) - 1]
        else
            "${dayFmt.get(Calendar.DAY_OF_MONTH)}"
        VitalsBar(
            label    = label,
            avgValue = if (slice.isEmpty()) null else slice.sumOf { it.value } / slice.size,
            minValue = slice.minOfOrNull { it.value },
            maxValue = slice.maxOfOrNull { it.value },
            present  = slice.isNotEmpty()
        )
    }
}
