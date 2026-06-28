package space.linuxct.pulseloop.ble

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.MainActivity
import space.linuxct.pulseloop.PulseLoopApp
import space.linuxct.pulseloop.R
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

private const val NOT_WORN_BPM = 80
private const val NOT_WORN_WINDOW = 3
private const val NOTIF_ID_RING_NOT_WORN = 4001

// Estimated metrics gated behind the Settings opt-in (default off).
private val BLOOD_METRIC_KINDS = setOf(
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_PRESSURE_SYSTOLIC,
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_PRESSURE_DIASTOLIC,
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_SUGAR,
)

@Singleton
class EventPersistenceSubscriber @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepo: DeviceRepository,
    private val measurementRepo: MeasurementRepository,
    private val activityRepo: ActivityRepository,
    private val sleepRepo: SleepRepository,
    private val debugRepo: DebugRepository,
    private val prefs: space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Days already zeroed-and-reset for the current ring history sync run
    private val activityDaysResetThisRun = mutableSetOf<Long>()

    // Master opt-in for the ring's estimated blood-pressure / blood-sugar. When OFF (the default),
    // those measurements are dropped here so nothing is written to the DB. Tracked live so flipping
    // the toggle in Settings starts capture from that moment on.
    @Volatile private var bloodMetricsEnabled = false

    // Emits after a health-data event has been persisted, so consumers (e.g. the OTLP exporter)
    // can react to "new data landed in the DB". Conflated — a burst collapses to one tick.
    private val _dataPersisted = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val dataPersisted: SharedFlow<Unit> = _dataPersisted

    // Sliding window of the last NOT_WORN_WINDOW live HR readings (oldest first).
    // Used to detect the jring sentinel value (80 bpm) that the ring emits when not worn.
    private val recentHrBpm = ArrayDeque<Int>(NOT_WORN_WINDOW)
    private var hrDropping = false

    fun start() {
        scope.launch {
            prefs.bloodMetricsEnabled.collect { bloodMetricsEnabled = it }
        }
        scope.launch {
            PulseEventBus.events.collect { event ->
                persist(event)
                if (event.isHealthData()) _dataPersisted.tryEmit(Unit)
            }
        }
    }

    /** Events that write health data worth re-exporting. Pure signal/UI events are excluded. */
    private fun PulseEvent.isHealthData(): Boolean = when (this) {
        is PulseEvent.HistoryMeasurement,
        is PulseEvent.HeartRateSample,
        is PulseEvent.Spo2Result,
        is PulseEvent.StressSample,
        is PulseEvent.HrvSample,
        is PulseEvent.TemperatureSample,
        is PulseEvent.ActivityUpdate,
        is PulseEvent.ActivityBucket,
        is PulseEvent.SleepTimeline,
        is PulseEvent.WorkoutFinished,
        is PulseEvent.GpsPoint -> true
        else -> false
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
            is PulseEvent.FirmwareVersion -> {
                val device = deviceRepo.getDevice() ?: return
                // The ring re-emits 0x0C status periodically; only write when it actually changes.
                if (device.firmwareVersion != event.version) {
                    deviceRepo.upsert(device.copy(firmwareVersion = event.version))
                }
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
            is PulseEvent.HeartRateSample -> persistLiveHR(event.bpm, event.timestamp)
            is PulseEvent.Spo2Result ->
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.SPO2, event.value.toDouble(), "%", event.timestamp, MeasurementSource.LIVE)
            is PulseEvent.HistoryMeasurement -> {
                // Estimated blood pressure / blood sugar are opt-in: drop them unless the user has
                // enabled the feature in Settings, so nothing is written to the DB by default.
                if (bloodMetricsEnabled || event.kind !in BLOOD_METRIC_KINDS) {
                    persistMeasurement(event.kind, event.value, event.kind.unit, event.timestamp, MeasurementSource.HISTORY)
                }
            }
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
        val dayMs = if (event.timestamp in (now - 2 * 86_400_000L)..(now + 60_000L)) {
            dayMidnightFor(event.timestamp)
        } else {
            todayMidnightMs()
        }

        if (event.steps <= 0) return  // ring sent 0 — can't derive anything useful

        val existing = activityRepo.getForDate(dayMs)

        if (existing == null) {
            // First record for this calendar day.
            // The jring ring sends a CUMULATIVE step total since its last hardware reset
            // (not since midnight). Detect this by reconstructing yesterday's final ring
            // reading and comparing: if today's reading already exceeds it, the ring
            // hasn't reset yet — store the daily delta instead of the raw value.
            val prev = activityRepo.getForDate(dayMs - 86_400_000L)
            // Reconstruct the ring's cumulative value at the end of the previous day:
            // baseline + whatever was counted in the last window of that day.
            val prevRingReading = if (prev != null) prev.stepBaseline + (prev.steps - prev.stepsSaved) else 0
            val (stepsToStore, baseline) = when {
                prev == null                      -> Pair(event.steps, 0)
                event.steps == prevRingReading    -> return   // stale / identical reading
                event.steps > prevRingReading     -> Pair(event.steps - prevRingReading, prevRingReading)
                else                              -> Pair(event.steps, 0)  // ring reset before midnight
            }
            activityRepo.upsert(ActivityDailyEntity(
                id = newUUID(), date = dayMs,
                steps = stepsToStore, stepBaseline = baseline, stepsSaved = 0,
                calories = event.calories, distanceMeters = event.distanceMeters,
                activeMinutes = 0, source = ActivityService.RING_HISTORY_SOURCE,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            // Reconstruct the ring's last known cumulative value for this day.
            // Formula: baseline (window start) + delta walked in this window.
            val lastRingValue = existing.stepBaseline + (existing.steps - existing.stepsSaved)

            val isReset = event.steps < existing.stepBaseline || event.steps < lastRingValue / 2

            val (newSteps, newBaseline, newSaved) = if (isReset) {
                // Ring hardware reset mid-day. Save the full daily total accumulated so
                // far into stepsSaved, then open a new window from 0. Add the ring's
                // current reading (steps walked since the reset) immediately so nothing
                // is lost even if we didn't catch the ring at exactly 0.
                Triple(existing.steps + event.steps, 0, existing.steps)
            } else {
                val candidate = existing.stepsSaved + (event.steps - existing.stepBaseline)
                if (candidate >= existing.steps) {
                    Triple(candidate, existing.stepBaseline, existing.stepsSaved)
                } else {
                    // Minor jitter — ring reading dipped slightly, keep current total.
                    Triple(existing.steps, existing.stepBaseline, existing.stepsSaved)
                }
            }

            activityRepo.upsert(existing.copy(
                steps = newSteps,
                stepBaseline = newBaseline,
                stepsSaved = newSaved,
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

    private suspend fun persistLiveHR(bpm: Int, timestamp: Long) {
        if (hrDropping) {
            if (bpm != NOT_WORN_BPM) {
                // Ring is worn again — resume normal storage and reset tracking state.
                hrDropping = false
                recentHrBpm.clear()
                recentHrBpm.addLast(bpm)
                persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.HEART_RATE, bpm.toDouble(), "bpm", timestamp, MeasurementSource.LIVE)
            }
            // Else: still getting 80 bpm — keep dropping.
            return
        }

        // Not in drop mode: check if the last NOT_WORN_WINDOW readings are all the sentinel value.
        if (recentHrBpm.size == NOT_WORN_WINDOW && recentHrBpm.all { it == NOT_WORN_BPM }) {
            hrDropping = true
            sendRingNotWornNotification()
            // Drop current reading (may be 80 or non-80; in either case we wait for the
            // ring to produce a confirmed non-sentinel value before storing again).
            return
        }

        // Normal: store and update the sliding window.
        if (recentHrBpm.size == NOT_WORN_WINDOW) recentHrBpm.removeFirst()
        recentHrBpm.addLast(bpm)
        persistMeasurement(space.linuxct.pulseloop.domain.model.MeasurementKind.HEART_RATE, bpm.toDouble(), "bpm", timestamp, MeasurementSource.LIVE)
    }

    private fun sendRingNotWornNotification() {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, PulseLoopApp.CHANNEL_HEALTH_ALERTS)
            .setSmallIcon(R.drawable.ic_workout)
            .setContentTitle(context.getString(R.string.notification_ring_not_worn_title))
            .setContentText(context.getString(R.string.notification_ring_not_worn_content))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.notification_ring_not_worn_content)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID_RING_NOT_WORN, notification)
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
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_PRESSURE_SYSTOLIC  -> "mmHg"
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_PRESSURE_DIASTOLIC -> "mmHg"
    space.linuxct.pulseloop.domain.model.MeasurementKind.BLOOD_SUGAR -> "mg/dL"
    space.linuxct.pulseloop.domain.model.MeasurementKind.FATIGUE     -> ""
}
