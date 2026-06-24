package space.linuxct.pulseloop.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import space.linuxct.pulseloop.domain.repository.DebugRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsSubscriber @Inject constructor(
    private val debugRepo: DebugRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            PulseEventBus.events.collect { event -> record(event) }
        }
    }

    private suspend fun record(event: PulseEvent) {
        val entry = when (event) {
            is PulseEvent.DeviceStateChanged ->
                WearableLogHelper.build("connection", "info", "State → ${event.state.rawValue}")
            is PulseEvent.DeviceIdentified ->
                WearableLogHelper.build("connection", "info", "Identified ${event.deviceType.rawValue}",
                    detail = event.capabilities.joinToString(",") { it.rawValue })
            is PulseEvent.BatteryLevel ->
                WearableLogHelper.build("battery", "info", "Battery ${event.percent}%")
            is PulseEvent.SyncProgress ->
                WearableLogHelper.build("sync", "info", "Sync: ${event.stage}")
            is PulseEvent.HeartRateSample ->
                WearableLogHelper.build("data", "info", "HR ${event.bpm} bpm")
            is PulseEvent.HeartRateComplete ->
                WearableLogHelper.build("sync", "info", "HR measurement complete")
            is PulseEvent.Spo2Result ->
                WearableLogHelper.build("data", "info", "SpO₂ ${event.value}%")
            is PulseEvent.Spo2Complete ->
                WearableLogHelper.build("sync", "info", "SpO₂ measurement complete")
            is PulseEvent.ActivityUpdate ->
                WearableLogHelper.build("data", "info", "Activity: ${event.steps} steps  ${event.calories.toInt()} kcal")
            is PulseEvent.HistoryMeasurement ->
                WearableLogHelper.build("data", "info", "${event.kind.rawValue}: ${event.value}")
            is PulseEvent.SleepTimeline ->
                WearableLogHelper.build("data", "info", "Sleep timeline: ${event.stages.size} segments")
            else -> return
        }
        debugRepo.insertLog(entry)
    }
}
