package space.linuxct.pulseloop.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.core.util.newUUID
import space.linuxct.pulseloop.core.util.todayMidnightMs
import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.DerivedUpdateRowEntity
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.domain.model.DecodeConfidence
import space.linuxct.pulseloop.domain.model.MeasurementSource
import space.linuxct.pulseloop.domain.model.SleepStage
import space.linuxct.pulseloop.domain.repository.ActivityRepository
import space.linuxct.pulseloop.domain.service.ActivityService
import space.linuxct.pulseloop.domain.repository.DebugRepository
import space.linuxct.pulseloop.domain.repository.DeviceRepository
import space.linuxct.pulseloop.domain.repository.MeasurementRepository
import space.linuxct.pulseloop.domain.repository.SleepRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventPersistenceSubscriber @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val measurementRepo: MeasurementRepository,
    private val activityRepo: ActivityRepository,
    private val sleepRepo: SleepRepository,
    private val debugRepo: DebugRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Days already zeroed-and-reset for the current ring history sync run
    private val activityDaysResetThisRun = mutableSetOf<Long>()

    fun start() {
        scope.launch {
            PulseEventBus.events.collect { persist(it) }
        }
    }

    private suspend fun persist(event: PulseEvent) {
        when (event) {
            is PulseEvent.DeviceStateChanged -> {
                val device = deviceRepo.getDevice() ?: return
                val updated = device.copy(
                    stateRaw = event.state.rawValue,
                    lastSeenAt = if (event.state == space.linuxct.pulseloop.domain.model.RingConnectionState.CONNECTED) System.currentTimeMillis() else device.lastSeenAt
                )
                deviceRepo.upsert(updated)
            }
            is PulseEvent.DeviceIdentified -> {
                val device = deviceRepo.getDevice() ?: return
                val updated = device.copy(
                    deviceTypeRaw = event.deviceType.rawValue,
                    capabilitiesRaw = event.capabilities.joinToString(",") { it.rawValue }
                )
                deviceRepo.upsert(updated)
            }
            is PulseEvent.BatteryLevel -> {
                val device = deviceRepo.getDevice() ?: return
                deviceRepo.upsert(device.copy(batteryLevel = event.percent))
            }
            is PulseEvent.RawPacket -> {
                debugRepo.insertRawPacket(
                    space.linuxct.pulseloop.data.db.entities.RawPacketRowEntity(
                        id = newUUID(),
                        timestamp = System.currentTimeMillis(),
                        directionRaw = event.direction.rawValue,
                        commandId = event.data.firstOrNull()?.toInt()?.and(0xFF) ?: 0,
                        hexPayload = event.data.joinToString("") { "%02x".format(it) },
                        confidenceRaw = DecodeConfidence.UNKNOWN.rawValue,
                        characteristicUuid = "",
                        deviceTypeRaw = "unknown"
                    )
                )
            }
            is PulseEvent.ActivityUpdate -> applyActivityUpdate(event)
            is PulseEvent.ActivityBucket -> applyActivityBucket(event)
            is PulseEvent.ActivitySyncReset -> activityDaysResetThisRun.clear()
            is PulseEvent.HeartRateSample ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.HEART_RATE, event.bpm.toDouble(), "bpm", event.timestamp, MeasurementSource.LIVE)
            is PulseEvent.Spo2Result ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.SPO2, event.value.toDouble(), "%", event.timestamp, MeasurementSource.LIVE)
            is PulseEvent.HistoryMeasurement ->
                persistMeasurement(event.kind, event.value, event.kind.unit, event.timestamp, MeasurementSource.HISTORY)
            is PulseEvent.StressSample ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.STRESS, event.value.toDouble(), "", event.timestamp, MeasurementSource.COLMI)
            is PulseEvent.HrvSample ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.HRV, event.value.toDouble(), "ms", event.timestamp, MeasurementSource.COLMI)
            is PulseEvent.TemperatureSample ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.TEMPERATURE, event.celsius, "°C", event.timestamp, MeasurementSource.COLMI)
            is PulseEvent.SleepTimeline ->
                persistSleepTimeline(event.timestamp, event.stages)
            is PulseEvent.GpsPoint -> {
                // GPS points are persisted by ActivityRecorderService/LiveWorkoutManager
            }
            is PulseEvent.SyncProgress -> {
                if (event.stage == "done") {
                    val device = deviceRepo.getDevice() ?: return
                    deviceRepo.upsert(device.copy(lastSeenAt = System.currentTimeMillis()))
                }
            }
            is PulseEvent.HeartRateComplete, is PulseEvent.Spo2Progress, is PulseEvent.Spo2Complete,
            is PulseEvent.WorkoutStarted, is PulseEvent.WorkoutPaused,
            is PulseEvent.WorkoutResumed, is PulseEvent.WorkoutFinished, is PulseEvent.CoachTrace,
            is PulseEvent.DerivedUpdate -> Unit
        }
    }

    private suspend fun applyActivityUpdate(event: PulseEvent.ActivityUpdate) {
        val now = System.currentTimeMillis()
        // Use the ring's own timestamp to determine the calendar day so that a sync
        // happening shortly after midnight (before the ring resets its counter) doesn't
        // incorrectly write yesterday's total into today's bucket. The ring's clock is
        // synced to the phone at startup, so its timestamp is reliable. Fall back to
        // today if the ring sends an invalid/un-synced timestamp.
        val dayMs = if (event.timestamp in (now - 2 * 86_400_000L)..(now + 60_000L)) {
            dayMidnightFor(event.timestamp)
        } else {
            todayMidnightMs()
        }

        val existing = activityRepo.getForDate(dayMs)

        if (existing == null) {
            // First record for this day. Before inserting, detect a cross-day carry-over:
            // some rings delay their midnight counter reset, so the first reading of a new
            // day reports the same total as the previous day. Skip the write and wait for
            // the next sync once the ring's counter has actually reset.
            val prevDayMs = dayMs - 86_400_000L
            val prev = activityRepo.getForDate(prevDayMs)
            if (prev != null && event.steps > 0 && event.steps == prev.steps) {
                return
            }
            activityRepo.upsert(ActivityDailyEntity(
                id = newUUID(), date = dayMs,
                steps = event.steps, calories = event.calories, distanceMeters = event.distanceMeters,
                activeMinutes = 0, source = ActivityService.RING_HISTORY_SOURCE, updatedAt = System.currentTimeMillis()
            ))
        } else {
            // Update an existing record. The ring's step counter is monotonically
            // increasing within a day, so a significant drop signals a delayed midnight
            // reset — trust the new lower value instead of clamping with maxOf.
            val newSteps = when {
                event.steps <= 0               -> existing.steps          // zero guard: transient glitch
                event.steps >= existing.steps  -> event.steps             // normal accumulation
                event.steps < existing.steps / 2 -> event.steps          // >50 % drop → delayed reset
                else                           -> existing.steps          // minor jitter, keep stored
            }
            activityRepo.upsert(existing.copy(
                steps = newSteps,
                calories = maxOf(existing.calories, event.calories),
                distanceMeters = maxOf(existing.distanceMeters, event.distanceMeters),
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    private suspend fun applyActivityBucket(event: PulseEvent.ActivityBucket) {
        val dayMs = dayMidnightFor(event.timestamp)
        val resetThisDay = !activityDaysResetThisRun.contains(dayMs)
        if (resetThisDay) activityDaysResetThisRun.add(dayMs)
        val existing = activityRepo.getForDate(dayMs)
        if (existing == null || resetThisDay) {
            activityRepo.upsert(ActivityDailyEntity(
                id = existing?.id ?: newUUID(), date = dayMs,
                steps = event.steps, calories = existing?.calories ?: 0.0,
                distanceMeters = event.distanceMeters, activeMinutes = 0,
                source = ActivityService.RING_HISTORY_SOURCE, updatedAt = System.currentTimeMillis()
            ))
        } else {
            activityRepo.upsert(existing.copy(
                steps = existing.steps + event.steps,
                distanceMeters = existing.distanceMeters + event.distanceMeters,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    private suspend fun persistMeasurement(
        kind: space.linuxct.pulseloop.domain.model.MeasurementKind,
        value: Double, unit: String, timestamp: Long, source: MeasurementSource
    ) {
        measurementRepo.insert(kind, value, unit, timestamp, source, DecodeConfidence.KNOWN)
        debugRepo.insertDerivedUpdate(DerivedUpdateRowEntity(
            id = newUUID(), timestamp = System.currentTimeMillis(),
            eventType = kind.rawValue, detail = null
        ))
    }

    private suspend fun persistSleepTimeline(startMs: Long, stages: List<SleepStage>) {
        val dayMs = dayMidnightFor(startMs)

        val session = sleepRepo.getSessionForDate(dayMs) ?: run {
            val new = SleepSessionEntity(
                id = newUUID(), startAt = startMs, endAt = startMs,
                score = null, syncedAt = System.currentTimeMillis(), deviceId = null
            )
            sleepRepo.upsertSession(new)
            new
        }

        val existing = sleepRepo.getExistingBlockStartTimes(session.id)
        var offset = 0
        while (offset < stages.size) {
            val stage = stages[offset]
            var duration = 1
            while (offset + duration < stages.size && stages[offset + duration] == stage) duration++
            val blockStart = startMs + offset * 60_000L
            if (!existing.contains(blockStart)) {
                sleepRepo.insertBlock(SleepStageBlockEntity(
                    id = newUUID(), sessionId = session.id, stageRaw = stage.rawValue,
                    startAt = blockStart, durationMinutes = duration
                ))
            }
            offset += duration
        }

        val blocks = sleepRepo.getBlocksForSession(session.id).sortedBy { it.startAt }
        if (blocks.isNotEmpty()) {
            val sessionStart = blocks.first().startAt
            val sessionEnd = blocks.maxOf { it.startAt + it.durationMinutes * 60_000L }
            // @Update (not upsertSession) — INSERT OR REPLACE would CASCADE-delete all blocks
            // via the FK before re-inserting the session row.
            sleepRepo.updateSession(session.copy(startAt = sessionStart, endAt = sessionEnd, syncedAt = System.currentTimeMillis()))
        }
    }

    private fun dayMidnightFor(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

private val space.linuxct.pulseloop.domain.model.MeasurementKind.unit: String get() = when (this) {
    space.linuxct.pulseloop.domain.model.MeasurementKind.HEART_RATE -> "bpm"
    space.linuxct.pulseloop.domain.model.MeasurementKind.SPO2       -> "%"
    space.linuxct.pulseloop.domain.model.MeasurementKind.STRESS     -> ""
    space.linuxct.pulseloop.domain.model.MeasurementKind.HRV        -> "ms"
    space.linuxct.pulseloop.domain.model.MeasurementKind.TEMPERATURE -> "°C"
}
