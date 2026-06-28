package space.linuxct.pulseloop.coach.tools

import org.json.JSONArray
import org.json.JSONObject
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.dayBoundsMs
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.downsample
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.downsampleList
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.isoString
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.localDateString
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.parseLocalDate
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.resolveRange
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.stats
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.todayMidnightMs
import space.linuxct.pulseloop.domain.service.MetricsService

object RetrievalTools {

    fun build(): List<CoachTool> = listOf(
        getProfileContext(),
        getDailySummary(),
        getRangeSummary(),
        getMetricSeries(),
        getActivitySessions(),
        getWorkoutDetail(),
        getSyncStatus(),
        getDeviceStatus(),
        getDataAvailability(),
        getSleepTrends(),
        getSleepDetail(),
        getGoalProgress()
    )

    // Shared description fragment for tools that accept an absolute date range.
    private const val RANGE_NOTE =
        "Provide an absolute historic window with start_date/end_date (yyyy-MM-dd, end inclusive), " +
        "or a relative look-back via the numeric field. start_date wins when present; null end_date means 'up to today'. " +
        "Call get_data_availability first to learn the earliest/latest stored dates."

    // ── Tool specs ─────────────────────────────────────────────────────────

    private const val NO_PARAMS =
        """{"type":"object","properties":{},"required":[],"additionalProperties":false}"""

    private const val SPEC_PROFILE_CONTEXT =
        """{"type":"function","name":"get_profile_context","description":"Returns the user's profile (name, age, sex, height, weight) and health goals (daily steps, sleep hours, active minutes).","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_DAILY_SUMMARY =
        """{"type":"function","name":"get_daily_summary","description":"Returns step count, calories, distance, active minutes, and latest heart rate / SpO2 readings for a specific date. Use 'today' for the current day.","parameters":{"type":"object","properties":{"date":{"type":"string","description":"Date in yyyy-MM-dd format, or the string 'today'"}},"required":["date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_RANGE_SUMMARY =
        """{"type":"function","name":"get_range_summary","description":"Returns averaged activity and vitals metrics over a time window (relative or absolute). $RANGE_NOTE","parameters":{"type":"object","properties":{"days":{"type":["integer","null"],"description":"Relative look-back in days from today (e.g. 7). Pass null when using start_date/end_date."},"start_date":{"type":["string","null"],"description":"Absolute window start, yyyy-MM-dd. Null to use 'days' instead."},"end_date":{"type":["string","null"],"description":"Absolute window end (inclusive), yyyy-MM-dd. Null means 'up to today'."}},"required":["days","start_date","end_date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_METRIC_SERIES =
        """{"type":"function","name":"get_metric_series","description":"Returns a time-series of readings for a single metric over a relative or absolute window. Useful for trend analysis and charts. $RANGE_NOTE","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":["hr","spo2","hrv","stress","temp","bp_sys","bp_dia","glucose","fatigue","steps","calories","distance","active_minutes"],"description":"The metric to retrieve"},"days":{"type":["integer","null"],"description":"Relative look-back in days. Pass null when using start_date/end_date."},"start_date":{"type":["string","null"],"description":"Absolute window start, yyyy-MM-dd. Null to use 'days'."},"end_date":{"type":["string","null"],"description":"Absolute window end (inclusive), yyyy-MM-dd. Null means 'up to today'."}},"required":["metric","days","start_date","end_date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_ACTIVITY_SESSIONS =
        """{"type":"function","name":"get_activity_sessions","description":"Returns recorded workout sessions (runs, walks, cycling, etc.) within a relative or absolute window, ordered by recency. Each item includes a session id usable with get_workout_detail. $RANGE_NOTE","parameters":{"type":"object","properties":{"days":{"type":["integer","null"],"description":"Relative look-back in days. Pass null when using start_date/end_date."},"start_date":{"type":["string","null"],"description":"Absolute window start, yyyy-MM-dd. Null to use 'days'."},"end_date":{"type":["string","null"],"description":"Absolute window end (inclusive), yyyy-MM-dd. Null means 'up to today'."},"limit":{"type":"integer","description":"Maximum number of sessions to return (1–50). Use 5 for a brief summary."}},"required":["days","start_date","end_date","limit"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_WORKOUT_DETAIL =
        """{"type":"function","name":"get_workout_detail","description":"Returns the full detail of one recorded workout session: summary totals, per-sample heart-rate and SpO2 series, a GPS route summary, and key session events. Get the session_id from get_activity_sessions.","parameters":{"type":"object","properties":{"session_id":{"type":"string","description":"The workout session id returned by get_activity_sessions."}},"required":["session_id"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_SYNC_STATUS =
        """{"type":"function","name":"get_sync_status","description":"Returns the current ring connection state and the timestamp of the last successful data sync.","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_DEVICE_STATUS =
        """{"type":"function","name":"get_device_status","description":"Returns the paired ring's current hardware status: name, model/type, battery level, firmware and hardware version, connection state, capabilities, and when it was last seen and paired.","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_DATA_AVAILABILITY =
        """{"type":"function","name":"get_data_availability","description":"Returns what health data exists in the database: per-metric reading counts AND the earliest/latest date stored for each. Call this first to learn the full historic span before requesting an absolute date range.","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_SLEEP_TRENDS =
        """{"type":"function","name":"get_sleep_trends","description":"Returns per-night sleep data (total duration, stage breakdown, sleep scores) over a relative or absolute window. $RANGE_NOTE","parameters":{"type":"object","properties":{"nights":{"type":["integer","null"],"description":"Relative number of nights to retrieve (e.g. 7). Pass null when using start_date/end_date."},"start_date":{"type":["string","null"],"description":"Absolute window start, yyyy-MM-dd. Null to use 'nights'."},"end_date":{"type":["string","null"],"description":"Absolute window end (inclusive), yyyy-MM-dd. Null means 'up to today'."}},"required":["nights","start_date","end_date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_SLEEP_DETAIL =
        """{"type":"function","name":"get_sleep_detail","description":"Returns the detailed stage-by-stage timeline for the sleep session of a single night (each block's stage, start time, and duration), plus stage totals and score.","parameters":{"type":"object","properties":{"date":{"type":"string","description":"The night to inspect in yyyy-MM-dd format (the calendar date the sleep session started on), or 'today' for the most recent night."}},"required":["date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_GOAL_PROGRESS =
        """{"type":"function","name":"get_goal_progress","description":"Returns today's progress toward daily health goals (steps, active minutes, calories) as absolute values and percentages.","parameters":$NO_PARAMS,"strict":true}"""

    // ── Handlers ───────────────────────────────────────────────────────────

    private fun getProfileContext() = CoachTool(
        name = "get_profile_context",
        specJson = SPEC_PROFILE_CONTEXT,
        run = { _, ctx ->
            val profile = ctx.profileDao.getProfile()
            val goals   = ctx.profileDao.getGoals()
            val obj = JSONObject().apply {
                put("name",         profile?.name)
                put("age",          profile?.age)
                put("sex",          profile?.biologicalSex)
                put("height_cm",    profile?.heightCm)
                put("weight_kg",    profile?.weightKg)
                put("profile_completeness", when {
                    profile == null -> "empty"
                    listOf(profile.name, profile.age, profile.heightCm, profile.weightKg, profile.biologicalSex).all { it != null } -> "complete"
                    else -> "partial"
                })
                put("goals", if (goals != null) JSONObject().apply {
                    put("daily_steps",      goals.dailySteps)
                    put("sleep_minutes",    goals.sleepMinutes)
                    put("active_minutes",   goals.activeMinutes)
                } else JSONObject.NULL)
            }
            ToolResult.success(obj)
        }
    )

    private fun getDailySummary() = CoachTool(
        name = "get_daily_summary",
        specJson = SPEC_DAILY_SUMMARY,
        run = { args, ctx ->
            val date  = JSONObject(args).optString("date", "today")
            val dayMs = parseLocalDate(date)
            val (start, end) = dayBoundsMs(dayMs)
            val isToday = start == todayMidnightMs()

            val activity     = ctx.activityDailyDao.getForDate(start)
            val profile      = ctx.profileDao.getProfile()
            val hrReadings   = ctx.measurementDao.getByKindSince("hr",   start).filter { it.timestamp <= end }
            val spo2Readings = ctx.measurementDao.getByKindSince("spo2", start).filter { it.timestamp <= end }

            val calories = MetricsService.computeCalories(activity, profile, isToday)

            val obj = JSONObject().apply {
                put("date",            localDateString(start))
                put("steps",           activity?.steps)
                put("calories",        calories)
                put("calories_source", MetricsService.caloriesSource(activity))
                put("distance_m",      activity?.distanceMeters)
                put("active_minutes",  activity?.activeMinutes)
                put("hr_readings",     hrReadings.size)
                put("latest_hr",       hrReadings.maxByOrNull { it.timestamp }?.value)
                put("avg_hr",          if (hrReadings.isEmpty()) null else hrReadings.map { it.value }.average())
                put("min_hr",          hrReadings.minOfOrNull { it.value })
                put("max_hr",          hrReadings.maxOfOrNull { it.value })
                put("spo2_readings",   spo2Readings.size)
                put("latest_spo2",     spo2Readings.maxByOrNull { it.timestamp }?.value)
                put("avg_spo2",        if (spo2Readings.isEmpty()) null else spo2Readings.map { it.value }.average())
                put("data_source",     activity?.source ?: "none")
            }
            ToolResult.success(obj)
        }
    )

    private fun getRangeSummary() = CoachTool(
        name = "get_range_summary",
        specJson = SPEC_RANGE_SUMMARY,
        run = { args, ctx ->
            val range   = resolveRange(JSONObject(args), "days", 7)
            val rows    = ctx.activityDailyDao.getBetween(range.startMs, range.endMs)
            val hrRows  = ctx.measurementDao.getByKindBetween("hr", range.startMs, range.endMs)
            val spo2Rows = ctx.measurementDao.getByKindBetween("spo2", range.startMs, range.endMs)
            val profile  = ctx.profileDao.getProfile()
            val todayMs  = todayMidnightMs()

            val stepVals   = rows.map { it.steps.toDouble() }
            val calVals    = rows.map { row ->
                MetricsService.computeCalories(row, profile, row.date == todayMs) ?: row.calories
            }
            val activeVals = rows.map { it.activeMinutes.toDouble() }
            val hrVals     = hrRows.map { it.value }
            val spo2Vals   = spo2Rows.map { it.value }
            val bmrDays    = rows.count { MetricsService.caloriesSource(it) == "bmr_estimate" }

            val obj = JSONObject().apply {
                put("range",             range.label)
                put("days_available",    rows.size)
                put("steps",             JSONObject(stats(stepVals)))
                put("calories",          JSONObject(stats(calVals)))
                put("calories_source",   if (bmrDays == rows.size) "bmr_estimate" else if (bmrDays == 0) "ring" else "mixed")
                put("active_minutes",    JSONObject(stats(activeVals)))
                put("heart_rate",        JSONObject(stats(hrVals)))
                put("spo2",              JSONObject(stats(spo2Vals)))
                put("hr_sample_count",   hrVals.size)
                put("spo2_sample_count", spo2Vals.size)
            }
            ToolResult.success(obj)
        }
    )

    private fun getMetricSeries() = CoachTool(
        name = "get_metric_series",
        specJson = SPEC_METRIC_SERIES,
        run = { args, ctx ->
            val o      = JSONObject(args)
            val metric = o.optString("metric", "hr")
            val range  = resolveRange(o, "days", 7)

            val arr = JSONArray()
            // Backstop: never return estimated BP/blood-sugar when the feature is off (empty series).
            val blocked = !ctx.bloodMetricsEnabled && metric in setOf("bp_sys", "bp_dia", "glucose")
            if (!blocked) when (metric) {
                "hr", "spo2", "hrv", "stress", "temp", "bp_sys", "bp_dia", "glucose", "fatigue" -> {
                    val rows = downsample(
                        ctx.measurementDao.getByKindBetween(metric, range.startMs, range.endMs).sortedBy { it.timestamp }
                    )
                    for (r in rows) {
                        arr.put(JSONObject().put("ts", isoString(r.timestamp)).put("v", r.value))
                    }
                }
                "steps", "calories", "distance", "active_minutes" -> {
                    val rows    = ctx.activityDailyDao.getBetween(range.startMs, range.endMs).sortedBy { it.date }
                    val profile = if (metric == "calories") ctx.profileDao.getProfile() else null
                    val todayMs = todayMidnightMs()
                    for (r in rows) {
                        val v = when (metric) {
                            "steps"          -> r.steps.toDouble()
                            "calories"       -> MetricsService.computeCalories(r, profile, r.date == todayMs) ?: r.calories
                            "distance"       -> r.distanceMeters
                            "active_minutes" -> r.activeMinutes.toDouble()
                            else             -> 0.0
                        }
                        arr.put(JSONObject().put("ts", localDateString(r.date)).put("v", v))
                    }
                }
            }

            val obj = JSONObject().apply {
                put("metric", metric)
                put("range",  range.label)
                put("count",  arr.length())
                put("series", arr)
            }
            ToolResult.success(obj)
        }
    )

    private fun getActivitySessions() = CoachTool(
        name = "get_activity_sessions",
        specJson = SPEC_ACTIVITY_SESSIONS,
        run = { args, ctx ->
            val o     = JSONObject(args)
            val range = resolveRange(o, "days", 30)
            val limit = o.optInt("limit", 5).coerceIn(1, 50)

            val sessions = ctx.activitySessionDao.getAll()
                .filter { it.startedAt in range.startMs..range.endMs && it.statusRaw == "finished" }
                .sortedByDescending { it.startedAt }
                .take(limit)

            val arr = JSONArray()
            for (s in sessions) {
                val durationMin = s.finishedAt?.let { (it - s.startedAt - s.elapsedPausedMs) / 60_000 }
                arr.put(JSONObject().apply {
                    put("id",             s.id)
                    put("type",           s.activityType)
                    put("started_at",     isoString(s.startedAt))
                    put("duration_min",   durationMin)
                    put("steps",          s.totalSteps)
                    put("calories",       s.totalCalories)
                    put("distance_m",     s.totalDistanceMeters)
                    put("active_min",     s.totalActiveMinutes)
                    put("has_gps",        s.useGps)
                })
            }
            ToolResult.success(JSONObject().put("range", range.label).put("sessions", arr).put("count", arr.length()))
        }
    )

    private fun getWorkoutDetail() = CoachTool(
        name = "get_workout_detail",
        specJson = SPEC_WORKOUT_DETAIL,
        run = { args, ctx ->
            val sessionId = JSONObject(args).optString("session_id", "")
            val s = if (sessionId.isBlank()) null else ctx.activitySessionDao.getById(sessionId)
            if (s == null) {
                return@CoachTool ToolResult.error("No workout session found with id '$sessionId'")
            }

            val samples = ctx.activitySessionDao.getSamplesForSession(s.id)
            val hrSamples   = samples.filter { it.kindRaw == "hr" }.sortedBy { it.timestamp }
            val spo2Samples = samples.filter { it.kindRaw == "spo2" }.sortedBy { it.timestamp }

            fun seriesJson(rows: List<space.linuxct.pulseloop.data.db.entities.ActivitySampleEntity>): JSONArray {
                val arr = JSONArray()
                for (r in downsampleList(rows, 200)) {
                    arr.put(JSONObject().put("ts", isoString(r.timestamp)).put("v", r.value))
                }
                return arr
            }

            val hrVals   = hrSamples.map { it.value }
            val spo2Vals = spo2Samples.map { it.value }

            val gps = ctx.activitySessionDao.getGpsPointsForSession(s.id)
            val acceptedGps = gps.filter { it.accepted }
            val gpsJson = JSONObject().apply {
                put("point_count",    gps.size)
                put("accepted_count", acceptedGps.size)
                if (acceptedGps.isNotEmpty()) {
                    val first = acceptedGps.first(); val last = acceptedGps.last()
                    put("start_lat", first.latitude);  put("start_lon", first.longitude)
                    put("end_lat",   last.latitude);   put("end_lon",   last.longitude)
                    put("min_lat", acceptedGps.minOf { it.latitude })
                    put("max_lat", acceptedGps.maxOf { it.latitude })
                    put("min_lon", acceptedGps.minOf { it.longitude })
                    put("max_lon", acceptedGps.maxOf { it.longitude })
                    put("max_altitude_m", acceptedGps.mapNotNull { it.altitude }.maxOrNull())
                    put("min_altitude_m", acceptedGps.mapNotNull { it.altitude }.minOrNull())
                }
            }

            val events = ctx.activitySessionDao.getEventsForSession(s.id)
            val eventsArr = JSONArray()
            for (e in events.take(40)) {
                eventsArr.put(JSONObject()
                    .put("at", isoString(e.timestamp))
                    .put("type", e.eventType)
                    .put("detail", e.detail))
            }

            val durationMin = s.finishedAt?.let { (it - s.startedAt - s.elapsedPausedMs) / 60_000 }
            val obj = JSONObject().apply {
                put("id",            s.id)
                put("type",          s.activityType)
                put("status",        s.statusRaw)
                put("started_at",    isoString(s.startedAt))
                put("finished_at",   s.finishedAt?.let { isoString(it) })
                put("duration_min",  durationMin)
                put("used_gps",      s.useGps)
                put("total_steps",       s.totalSteps)
                put("total_calories",    s.totalCalories)
                put("total_distance_m",  s.totalDistanceMeters)
                put("total_active_min",  s.totalActiveMinutes)
                put("hr_sample_count",   hrSamples.size)
                put("avg_hr",   if (hrVals.isEmpty()) null else hrVals.average())
                put("min_hr",   hrVals.minOrNull())
                put("max_hr",   hrVals.maxOrNull())
                put("hr_series",   seriesJson(hrSamples))
                put("spo2_sample_count", spo2Samples.size)
                put("avg_spo2", if (spo2Vals.isEmpty()) null else spo2Vals.average())
                put("spo2_series", seriesJson(spo2Samples))
                put("gps_route",   gpsJson)
                put("events",      eventsArr)
                put("notes",       s.notes)
            }
            ToolResult.success(obj)
        }
    )

    private fun getSyncStatus() = CoachTool(
        name = "get_sync_status",
        specJson = SPEC_SYNC_STATUS,
        run = { _, ctx ->
            val allActivity = ctx.activityDailyDao.getAll()
            val allMeasurements = ctx.measurementDao.getByKind("hr")
            val lastActivityMs  = allActivity.maxOfOrNull { it.updatedAt }
            val lastMeasureMs   = allMeasurements.maxOfOrNull { it.timestamp }
            val lastSyncMs      = maxOf(lastActivityMs ?: 0L, lastMeasureMs ?: 0L).takeIf { it > 0L }

            val obj = JSONObject().apply {
                put("last_sync_at",       lastSyncMs?.let { isoString(it) })
                put("minutes_since_sync", lastSyncMs?.let { (System.currentTimeMillis() - it) / 60_000 })
                put("activity_days",      allActivity.size)
                put("hr_readings_total",  allMeasurements.size)
            }
            ToolResult.success(obj)
        }
    )

    private fun getDeviceStatus() = CoachTool(
        name = "get_device_status",
        specJson = SPEC_DEVICE_STATUS,
        run = { _, ctx ->
            val d = ctx.deviceDao.getDevice()
            if (d == null) {
                return@CoachTool ToolResult.success(JSONObject().put("paired", false))
            }
            val obj = JSONObject().apply {
                put("paired",            true)
                put("name",              d.name)
                put("device_type",       d.deviceTypeRaw)
                put("connection_state",  d.stateRaw)
                put("battery_level",     d.batteryLevel)
                put("firmware_version",  d.firmwareVersion)
                put("hardware_version",  d.hardwareVersion)
                put("capabilities",      d.capabilitiesRaw)
                put("last_seen_at",      d.lastSeenAt?.let { isoString(it) })
                put("paired_at",         isoString(d.pairedAt))
            }
            ToolResult.success(obj)
        }
    )

    private fun getDataAvailability() = CoachTool(
        name = "get_data_availability",
        specJson = SPEC_DATA_AVAILABILITY,
        run = { _, ctx ->
            // Per-metric span = earliest/latest timestamp, so the model can frame valid date ranges.
            suspend fun metricSpan(kind: String): JSONObject {
                val count = ctx.measurementDao.countByKind(kind)
                return JSONObject().apply {
                    put("count", count)
                    put("earliest", ctx.measurementDao.earliestTimestamp(kind)?.let { localDateString(it) })
                    put("latest",   ctx.measurementDao.latestTimestamp(kind)?.let { localDateString(it) })
                }
            }

            val activity = ctx.activityDailyDao.getAll()         // DESC by date
            val sleep    = ctx.sleepDao.getAllSessions()         // DESC by startAt

            val obj = JSONObject().apply {
                put("heart_rate",  metricSpan("hr"))
                put("spo2",        metricSpan("spo2"))
                put("hrv",         metricSpan("hrv"))
                put("stress",      metricSpan("stress"))
                put("temperature", metricSpan("temp"))
                if (ctx.bloodMetricsEnabled) {
                    put("blood_pressure_systolic",  metricSpan("bp_sys"))
                    put("blood_pressure_diastolic", metricSpan("bp_dia"))
                    put("blood_sugar", metricSpan("glucose"))
                }
                put("fatigue",     metricSpan("fatigue"))
                put("activity_daily", JSONObject().apply {
                    put("count",    activity.size)
                    put("earliest", activity.lastOrNull()?.date?.let { localDateString(it) })
                    put("latest",   activity.firstOrNull()?.date?.let { localDateString(it) })
                })
                put("sleep", JSONObject().apply {
                    put("count",    sleep.size)
                    put("earliest", sleep.lastOrNull()?.startAt?.let { localDateString(it) })
                    put("latest",   sleep.firstOrNull()?.startAt?.let { localDateString(it) })
                })
                put("workouts", ctx.activitySessionDao.getAll().count { it.statusRaw == "finished" })
            }
            ToolResult.success(obj)
        }
    )

    private fun getSleepTrends() = CoachTool(
        name = "get_sleep_trends",
        specJson = SPEC_SLEEP_TRENDS,
        run = { args, ctx ->
            val range = resolveRange(JSONObject(args), "nights", 7)

            val sessions = ctx.sleepDao.getAllSessions()
                .filter { it.startAt in range.startMs..range.endMs }
                .sortedByDescending { it.startAt }

            val arr = JSONArray()
            for (s in sessions) {
                val durationMin = ((s.endAt - s.startAt) / 60_000).toInt()
                val blocks = ctx.sleepDao.getBlocksForSession(s.id)
                arr.put(JSONObject().apply {
                    put("date",           localDateString(s.startAt))
                    put("total_min",      durationMin)
                    put("score",          s.score)
                    put("deep_min",       blocks.filter { it.stageRaw == "deep" }.sumOf { it.durationMinutes })
                    put("light_min",      blocks.filter { it.stageRaw == "light" }.sumOf { it.durationMinutes })
                    put("rem_min",        blocks.filter { it.stageRaw == "rem" }.sumOf { it.durationMinutes })
                    put("awake_min",      blocks.filter { it.stageRaw == "awake" }.sumOf { it.durationMinutes })
                })
            }

            val totalMins = sessions.map { ((it.endAt - it.startAt) / 60_000).toDouble() }
            val obj = JSONObject().apply {
                put("range",            range.label)
                put("nights_available", sessions.size)
                put("sessions",         arr)
                put("avg_duration_min", if (totalMins.isEmpty()) null else totalMins.average())
            }
            ToolResult.success(obj)
        }
    )

    private fun getSleepDetail() = CoachTool(
        name = "get_sleep_detail",
        specJson = SPEC_SLEEP_DETAIL,
        run = { args, ctx ->
            val date = JSONObject(args).optString("date", "today")
            val sessions = ctx.sleepDao.getAllSessions()
            val target = if (date.equals("today", ignoreCase = true)) {
                sessions.maxByOrNull { it.startAt }
            } else {
                val wanted = localDateString(parseLocalDate(date))
                sessions.firstOrNull { localDateString(it.startAt) == wanted }
            }
            if (target == null) {
                return@CoachTool ToolResult.success(
                    JSONObject().put("found", false).put("requested_date", date)
                )
            }

            val blocks = ctx.sleepDao.getBlocksForSession(target.id).sortedBy { it.startAt }
            val blocksArr = JSONArray()
            for (b in blocks) {
                blocksArr.put(JSONObject().apply {
                    put("stage",        b.stageRaw)
                    put("start",        isoString(b.startAt))
                    put("duration_min", b.durationMinutes)
                })
            }
            fun stageMin(stage: String) = blocks.filter { it.stageRaw == stage }.sumOf { it.durationMinutes }

            val obj = JSONObject().apply {
                put("found",      true)
                put("date",       localDateString(target.startAt))
                put("start",      isoString(target.startAt))
                put("end",        isoString(target.endAt))
                put("total_min",  ((target.endAt - target.startAt) / 60_000).toInt())
                put("score",      target.score)
                put("deep_min",   stageMin("deep"))
                put("light_min",  stageMin("light"))
                put("rem_min",    stageMin("rem"))
                put("awake_min",  stageMin("awake"))
                put("block_count", blocks.size)
                put("blocks",     blocksArr)
            }
            ToolResult.success(obj)
        }
    )

    private fun getGoalProgress() = CoachTool(
        name = "get_goal_progress",
        specJson = SPEC_GOAL_PROGRESS,
        run = { _, ctx ->
            val goals   = ctx.profileDao.getGoals()
            val profile = ctx.profileDao.getProfile()
            val today   = ctx.activityDailyDao.getForDate(todayMidnightMs())

            val stepsGoal  = goals?.dailySteps   ?: 10_000
            val activeGoal = goals?.activeMinutes ?: 45
            val calories   = MetricsService.computeCalories(today, profile, isToday = true)

            val obj = JSONObject().apply {
                put("steps_goal",         stepsGoal)
                put("steps_today",        today?.steps ?: 0)
                put("steps_pct",          ((today?.steps ?: 0) * 100.0 / stepsGoal).toInt())
                put("active_min_goal",    activeGoal)
                put("active_min_today",   today?.activeMinutes ?: 0)
                put("active_min_pct",     (((today?.activeMinutes ?: 0) * 100.0) / activeGoal).toInt())
                put("calories_today",     calories)
                put("calories_source",    MetricsService.caloriesSource(today))
                put("data_available",     today != null)
            }
            ToolResult.success(obj)
        }
    )
}
