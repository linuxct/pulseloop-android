package space.linuxct.pulseloop.data.export.otlp

import space.linuxct.pulseloop.data.db.entities.ActivityDailyEntity
import space.linuxct.pulseloop.data.db.entities.ActivityGpsPointEntity
import space.linuxct.pulseloop.data.db.entities.ActivitySessionEntity
import space.linuxct.pulseloop.data.db.entities.DeviceEntity
import space.linuxct.pulseloop.data.db.entities.MeasurementEntity
import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity

/** A single OTLP gauge data point, ready to serialize. */
data class OtlpPoint(
    val metric: String,
    val unit: String,
    val timeUnixNano: Long,
    val value: Double,
    val attributes: Map<String, String>,
)

/**
 * Entity → OTLP gauge points. This is the ONLY place that decides what becomes a point attribute
 * (an indexed dimension), so the cardinality discipline is enforced here:
 * attributes are limited to the bounded `source` / `stage` / `activity_type` sets. Numeric values,
 * ids, and coordinates are point VALUES — never attributes. Mapping is deterministic (no clock/random)
 * so re-sent points are byte-identical and dedupe at the backend.
 */
object OtlpMetricMapper {

    private fun nanos(ms: Long): Long = ms * 1_000_000L

    private fun signal(kindRaw: String): String = when (kindRaw) {
        "hr" -> "heart_rate"
        "temp" -> "temperature"
        "bp_sys" -> "blood_pressure_systolic"
        "bp_dia" -> "blood_pressure_diastolic"
        "glucose" -> "blood_sugar"
        else -> kindRaw // spo2, hrv, stress, fatigue
    }

    private fun unitFor(kindRaw: String, raw: String): String = when {
        raw.isNotBlank() && raw != "°C" -> raw      // mmHg, mg/dL, %, ms, bpm
        kindRaw == "temp" -> "Cel"
        kindRaw == "stress" || kindRaw == "fatigue" -> "1"
        else -> if (raw == "°C") "Cel" else "1"
    }

    fun measurement(m: MeasurementEntity): OtlpPoint = OtlpPoint(
        metric = "health.${signal(m.kindRaw)}",
        unit = unitFor(m.kindRaw, m.unit),
        timeUnixNano = nanos(m.timestamp),
        value = m.value,
        attributes = mapOf("source" to m.sourceRaw),
    )

    fun activityDaily(d: ActivityDailyEntity): List<OtlpPoint> {
        val ts = nanos(d.date)
        val attrs = mapOf("source" to d.source)
        return listOf(
            OtlpPoint("health.steps.daily", "{steps}", ts, d.steps.toDouble(), attrs),
            OtlpPoint("health.calories.daily", "kcal", ts, d.calories, attrs),
            OtlpPoint("health.distance.daily", "m", ts, d.distanceMeters, attrs),
            OtlpPoint("health.active_minutes.daily", "min", ts, d.activeMinutes.toDouble(), attrs),
        )
    }

    private fun stageCode(stageRaw: String): Double = when (stageRaw) {
        "light" -> 1.0
        "deep" -> 2.0
        "rem" -> 3.0
        "awake" -> 4.0
        else -> 0.0 // unknown
    }

    fun sleepBlock(b: SleepStageBlockEntity): List<OtlpPoint> {
        val ts = nanos(b.startAt)
        val attrs = mapOf("stage" to b.stageRaw)
        return listOf(
            OtlpPoint("health.sleep.stage.duration", "min", ts, b.durationMinutes.toDouble(), attrs),
            OtlpPoint("health.sleep.stage.code", "1", ts, stageCode(b.stageRaw), attrs),
        )
    }

    fun sleepSession(s: SleepSessionEntity): List<OtlpPoint> {
        val ts = nanos(s.startAt)
        val out = mutableListOf(
            OtlpPoint("health.sleep.duration", "min", ts, (s.endAt - s.startAt) / 60_000.0, emptyMap()),
        )
        s.score?.let { out += OtlpPoint("health.sleep.score", "1", ts, it.toDouble(), emptyMap()) }
        return out
    }

    fun workout(s: ActivitySessionEntity): List<OtlpPoint> {
        val ts = nanos(s.startedAt)
        val attrs = mapOf("activity_type" to s.activityType)
        val durationMin = s.finishedAt?.let { (it - s.startedAt - s.elapsedPausedMs) / 60_000.0 } ?: 0.0
        return listOf(
            OtlpPoint("health.workout.duration", "min", ts, durationMin, attrs),
            OtlpPoint("health.workout.distance", "m", ts, s.totalDistanceMeters, attrs),
            OtlpPoint("health.workout.calories", "kcal", ts, s.totalCalories, attrs),
            OtlpPoint("health.workout.steps", "{steps}", ts, s.totalSteps.toDouble(), attrs),
            OtlpPoint("health.workout.active_minutes", "min", ts, s.totalActiveMinutes.toDouble(), attrs),
        )
    }

    /** Only accepted GPS points are exported. activity_type is the only (bounded) attribute. */
    fun gpsPoint(p: ActivityGpsPointEntity, activityType: String): List<OtlpPoint> {
        val ts = nanos(p.timestamp)
        val attrs = mapOf("activity_type" to activityType)
        val out = mutableListOf(
            OtlpPoint("health.gps.latitude", "deg", ts, p.latitude, attrs),
            OtlpPoint("health.gps.longitude", "deg", ts, p.longitude, attrs),
        )
        p.altitude?.let { out += OtlpPoint("health.gps.altitude", "m", ts, it, attrs) }
        p.speed?.let { out += OtlpPoint("health.gps.speed", "m/s", ts, it.toDouble(), attrs) }
        return out
    }

    fun device(d: DeviceEntity): List<OtlpPoint> {
        val battery = d.batteryLevel ?: return emptyList()
        val ts = d.lastSeenAt ?: return emptyList()
        return listOf(
            OtlpPoint("health.device.battery", "%", nanos(ts), battery.toDouble(), emptyMap()),
        )
    }
}
