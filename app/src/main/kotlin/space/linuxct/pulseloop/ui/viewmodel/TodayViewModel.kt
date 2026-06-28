package space.linuxct.pulseloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.domain.model.TodaySummary
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.repository.ProfileRepository
import space.linuxct.pulseloop.domain.repository.SleepRepository
import space.linuxct.pulseloop.domain.service.MetricsService
import space.linuxct.pulseloop.domain.service.SleepService
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val measurementRepo: MeasurementRepository,
    private val activityRepo: ActivityRepository,
    private val sleepRepo: SleepRepository,
    private val deviceRepo: DeviceRepository,
    private val profileRepo: ProfileRepository,
    private val prefs: AppPreferencesDataStore
) : ViewModel() {

    private val cutoff30d = System.currentTimeMillis() - 30L * 24 * 3_600_000

    private data class BloodCal(val bpSys: Int, val bpDia: Int, val glucoseOffset: Double, val enabled: Boolean)

    // Nest goals+profile+BP/glucose calibration+opt-in into one flow so we stay within the 5-flow combine limit
    private val goalsProfileCalibration = combine(
        profileRepo.observeGoals(),
        profileRepo.observeProfile(),
        combine(prefs.bpCalSystolic, prefs.bpCalDiastolic, prefs.glucoseOffsetMgdl, prefs.bloodMetricsEnabled) { s, d, g, e -> BloodCal(s, d, g, e) }
    ) { goals, profile, cal -> Triple(goals, profile, cal) }

    val todaySummary: StateFlow<TodaySummary?> = combine(
        activityRepo.observeAll(),
        sleepRepo.observeSessions(),
        deviceRepo.observeDevice(),
        goalsProfileCalibration,
        measurementRepo.observeSince(cutoff30d)
    ) { activityRows, sleepSessions, device, (goals, profile, cal), measurements ->
        val blocks = sleepSessions.flatMap { s -> sleepRepo.getBlocksForSession(s.id) }
        val blocksMap = blocks.groupBy { it.sessionId }
        val sleepSummary = SleepService.latestSleep(sleepSessions, blocksMap)
        val sleepBlocks = sleepSummary?.let { blocksMap[it.session.id] ?: emptyList() } ?: emptyList()

        MetricsService.buildTodaySummary(
            activityRows = activityRows,
            measurements = measurements,
            device = device,
            sleepSummary = sleepSummary,
            sleepBlocks = sleepBlocks,
            goals = goals,
            profile = profile,
            bpCalSystolic = cal.bpSys,
            bpCalDiastolic = cal.bpDia,
            glucoseOffsetMgdl = cal.glucoseOffset,
            bloodMetricsEnabled = cal.enabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
