package space.linuxct.pulseloop.data.export.otlp

import space.linuxct.pulseloop.data.db.dao.ActivityDailyDao
import space.linuxct.pulseloop.data.db.dao.ActivitySessionDao
import space.linuxct.pulseloop.data.db.dao.DeviceDao
import space.linuxct.pulseloop.data.db.dao.MeasurementDao
import space.linuxct.pulseloop.data.db.dao.OtlpExportStateDao
import space.linuxct.pulseloop.data.db.dao.SleepDao
import space.linuxct.pulseloop.data.db.entities.OtlpExportStateEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads health data from Room and pushes it to the OTLP endpoint as gauge metrics.
 *
 * Cursor model: per-type `lastTimestampMs` (a high-water by capture time). Each incremental run
 * re-scans `[lastTs - OVERLAP_MS, now]` (so backdated ring history within the overlap is caught;
 * TSDB dedupe absorbs the re-sends). On first run `lastTs == 0`, so the window is the entire
 * history — i.e. the first run IS the backfill, walked in [WINDOW_SPAN_MS] sub-windows under a
 * time budget and resumable via the per-window watermark advance.
 */
@Singleton
class OtlpExporter @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val activityDailyDao: ActivityDailyDao,
    private val activitySessionDao: ActivitySessionDao,
    private val sleepDao: SleepDao,
    private val deviceDao: DeviceDao,
    private val stateDao: OtlpExportStateDao,
    private val configStore: OtlpConfigStore,
    private val sender: OtlpHttpSender,
) {
    sealed interface Outcome {
        data class Done(val exported: Int, val rejected: Long) : Outcome
        data class MoreWork(val exported: Int) : Outcome
        data object Disabled : Outcome
        data object NotConfigured : Outcome
        data class Retry(val reason: String, val retryAfterMs: Long?) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    suspend fun testConnection(config: OtlpExportConfig): SendResult {
        val pt = OtlpPoint(
            metric = "health.export.heartbeat",
            unit = "1",
            timeUnixNano = System.currentTimeMillis() * 1_000_000L,
            value = 1.0,
            attributes = emptyMap(),
        )
        return sender.send(config, OtlpProtoSerializer.serialize(config, listOf(pt)))
    }

    suspend fun export(forceBackfill: Boolean, deadlineMs: Long): Outcome {
        val config = configStore.snapshot()
        if (!config.enabled) return Outcome.Disabled
        if (!config.isConfigured) return Outcome.NotConfigured

        val now = System.currentTimeMillis()
        var totalExported = 0
        var totalRejected = 0L

        val types = OtlpDataType.values().filter { it != OtlpDataType.GPS || config.includeGps }
        for (type in types) {
            val step = if (type in OtlpDataType.MEASUREMENT_TYPES) {
                processMeasurement(type, config, now, deadlineMs, forceBackfill)
            } else {
                processSimple(type, config, now, forceBackfill)
            }
            when (step) {
                is Step.Progress -> {
                    totalExported += step.exported
                    totalRejected += step.rejected
                }
                is Step.More -> return Outcome.MoreWork(totalExported + step.exported)
                is Step.Retry -> return Outcome.Retry(step.reason, step.retryAfterMs)
                is Step.Fail -> {
                    recordError(type, step.reason)
                    return Outcome.Failed(step.reason)
                }
            }
        }
        return Outcome.Done(totalExported, totalRejected)
    }

    // ── Per-type processing ─────────────────────────────────────────────────

    private sealed interface Step {
        data class Progress(val exported: Int, val rejected: Long) : Step
        data class More(val exported: Int) : Step
        data class Retry(val reason: String, val retryAfterMs: Long?) : Step
        data class Fail(val reason: String) : Step
    }

    /** High-volume measurement types: sub-window walk with per-window watermark advance + budget. */
    private suspend fun processMeasurement(
        type: OtlpDataType,
        config: OtlpExportConfig,
        now: Long,
        deadlineMs: Long,
        forceBackfill: Boolean,
    ): Step {
        val prev = stateDao.getOne(type.key)
        val lastTs = prev?.lastTimestampMs ?: 0L
        val earliest = measurementDao.earliestTimestamp(type.key)
        if (earliest == null) {
            advance(type, now, prev, 0); return Step.Progress(0, 0)
        }

        var windowStart = if (forceBackfill || lastTs == 0L) earliest
        else (lastTs - OVERLAP_MS).coerceAtLeast(earliest)

        var exported = 0
        var rejected = 0L
        var ran = false
        while (windowStart < now) {
            ran = true
            val windowEnd = (windowStart + WINDOW_SPAN_MS).coerceAtMost(now)
            val points = measurementDao.getByKindBetween(type.key, windowStart, windowEnd)
                .sortedBy { it.timestamp }
                .map { OtlpMetricMapper.measurement(it) }

            when (val r = sendChunks(config, points)) {
                is ChunkResult.Ok -> {
                    exported += r.exported; rejected += r.rejected
                    advance(type, windowEnd, prev, exported)
                }
                is ChunkResult.Retry -> return Step.Retry("${type.key}: server busy", r.retryAfterMs)
                is ChunkResult.Fail -> return Step.Fail("${type.key}: ${r.reason}")
            }

            windowStart = windowEnd
            if (System.currentTimeMillis() > deadlineMs && windowStart < now) {
                return Step.More(exported)
            }
        }
        if (!ran) advance(type, now, prev, 0)
        return Step.Progress(exported, rejected)
    }

    /** Low-volume types: a single window `[start, now]`. */
    private suspend fun processSimple(
        type: OtlpDataType,
        config: OtlpExportConfig,
        now: Long,
        forceBackfill: Boolean,
    ): Step {
        val prev = stateDao.getOne(type.key)
        val lastTs = prev?.lastTimestampMs ?: 0L
        val start = if (forceBackfill || lastTs == 0L) 0L else (lastTs - OVERLAP_MS).coerceAtLeast(0L)
        val points = collectSimple(type, start, now, config)

        return when (val r = sendChunks(config, points)) {
            is ChunkResult.Ok -> {
                advance(type, now, prev, r.exported)
                Step.Progress(r.exported, r.rejected)
            }
            is ChunkResult.Retry -> Step.Retry("${type.key}: server busy", r.retryAfterMs)
            is ChunkResult.Fail -> Step.Fail("${type.key}: ${r.reason}")
        }
    }

    private suspend fun collectSimple(
        type: OtlpDataType,
        start: Long,
        end: Long,
        config: OtlpExportConfig,
    ): List<OtlpPoint> = when (type) {
        OtlpDataType.ACTIVITY_DAILY ->
            activityDailyDao.getBetween(start, end).sortedBy { it.date }
                .flatMap { OtlpMetricMapper.activityDaily(it) }

        OtlpDataType.SLEEP ->
            sleepDao.getAllSessions().filter { it.startAt in start..end }.sortedBy { it.startAt }
                .flatMap { s ->
                    OtlpMetricMapper.sleepSession(s) +
                        sleepDao.getBlocksForSession(s.id).flatMap { OtlpMetricMapper.sleepBlock(it) }
                }

        OtlpDataType.WORKOUT ->
            activitySessionDao.getAll()
                .filter { it.statusRaw == "finished" && it.startedAt in start..end }
                .sortedBy { it.startedAt }
                .flatMap { OtlpMetricMapper.workout(it) }

        OtlpDataType.GPS ->
            if (!config.includeGps) emptyList()
            else activitySessionDao.getAll()
                .filter { it.statusRaw == "finished" && it.startedAt in start..end }
                .flatMap { s ->
                    activitySessionDao.getGpsPointsForSession(s.id)
                        .filter { it.accepted }
                        .flatMap { OtlpMetricMapper.gpsPoint(it, s.activityType) }
                }

        OtlpDataType.DEVICE ->
            deviceDao.getDevice()?.let { d ->
                if ((d.lastSeenAt ?: 0L) in start..end) OtlpMetricMapper.device(d) else emptyList()
            } ?: emptyList()

        else -> emptyList()
    }

    // ── Chunked sending ─────────────────────────────────────────────────────

    private sealed interface ChunkResult {
        data class Ok(val exported: Int, val rejected: Long) : ChunkResult
        data class Retry(val retryAfterMs: Long?) : ChunkResult
        data class Fail(val reason: String) : ChunkResult
    }

    private suspend fun sendChunks(config: OtlpExportConfig, points: List<OtlpPoint>): ChunkResult {
        if (points.isEmpty()) return ChunkResult.Ok(0, 0)
        var idx = 0
        var exported = 0
        var rejected = 0L
        var size = MAX_POINTS
        while (idx < points.size) {
            val end = (idx + size).coerceAtMost(points.size)
            val chunk = points.subList(idx, end)
            val payload = OtlpProtoSerializer.serialize(config, chunk)
            when (val r = sender.send(config, payload)) {
                is SendResult.Success -> {
                    exported += chunk.size; rejected += r.rejected; idx = end
                }
                SendResult.PayloadTooLarge -> {
                    if (size <= 1) return ChunkResult.Fail("payload too large")
                    size = (size / 2).coerceAtLeast(1) // retry same idx with a smaller chunk
                }
                is SendResult.Retryable -> return ChunkResult.Retry(r.retryAfterMs)
                is SendResult.Fatal -> return ChunkResult.Fail("${r.code}: ${r.message}")
            }
        }
        return ChunkResult.Ok(exported, rejected)
    }

    // ── State persistence ───────────────────────────────────────────────────

    private suspend fun advance(type: OtlpDataType, ts: Long, prev: OtlpExportStateEntity?, exportedDelta: Int) {
        stateDao.upsert(
            OtlpExportStateEntity(
                dataType = type.key,
                lastTimestampMs = ts,
                backfillCursorMs = 0,
                exportedCount = (prev?.exportedCount ?: 0L) + exportedDelta,
                lastError = null,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun recordError(type: OtlpDataType, reason: String) {
        val prev = stateDao.getOne(type.key)
        stateDao.upsert(
            OtlpExportStateEntity(
                dataType = type.key,
                lastTimestampMs = prev?.lastTimestampMs ?: 0L,
                backfillCursorMs = prev?.backfillCursorMs ?: 0L,
                exportedCount = prev?.exportedCount ?: 0L,
                lastError = reason.take(300),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private companion object {
        const val OVERLAP_MS = 12 * 60 * 60 * 1000L      // re-scan window for backdated inserts
        const val WINDOW_SPAN_MS = 7 * 24 * 60 * 60 * 1000L // backfill sub-window size
        const val MAX_POINTS = 2000                       // points per OTLP request (pre-gzip)
    }
}
