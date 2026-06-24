package space.linuxct.pulseloop.coach.tools

import org.json.JSONArray
import org.json.JSONObject
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.cutoffMs
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.dayBoundsMs
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.downsample
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.isoString
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.localDateString
import space.linuxct.pulseloop.coach.tools.CoachDataAccess.parseLocalDate
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
        getSyncStatus(),
        getDataAvailability(),
        getSleepTrends(),
        getGoalProgress()
    )

    // ── Tool specs ─────────────────────────────────────────────────────────

    private const val NO_PARAMS =
        """{"type":"object","properties":{},"required":[],"additionalProperties":false}"""

    private const val SPEC_PROFILE_CONTEXT =
        """{"type":"function","name":"get_profile_context","description":"Returns the user's profile (name, age, sex, height, weight) and health goals (daily steps, sleep hours, active minutes).","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_DAILY_SUMMARY =
        """{"type":"function","name":"get_daily_summary","description":"Returns step count, calories, distance, active minutes, and latest heart rate / SpO2 readings for a specific date. Use 'today' for the current day.","parameters":{"type":"object","properties":{"date":{"type":"string","description":"Date in yyyy-MM-dd format, or the string 'today'"}},"required":["date"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_RANGE_SUMMARY =
        """{"type":"function","name":"get_range_summary","description":"Returns averaged activity and vitals metrics over the last N days.","parameters":{"type":"object","properties":{"days":{"type":"integer","description":"Number of days to look back (1–90). Use 7 for a weekly summary."}},"required":["days"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_METRIC_SERIES =
        """{"type":"function","name":"get_metric_series","description":"Returns a time-series of readings for a single metric. Useful for trend analysis.","parameters":{"type":"object","properties":{"metric":{"type":"string","enum":["hr","spo2","hrv","stress","temp","steps","calories","distance","active_minutes"],"description":"The metric to retrieve"},"days":{"type":"integer","description":"Number of days to look back (1–90)"}},"required":["metric","days"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_ACTIVITY_SESSIONS =
        """{"type":"function","name":"get_activity_sessions","description":"Returns recorded workout sessions (runs, walks, cycling, etc.) ordered by recency.","parameters":{"type":"object","properties":{"days":{"type":"integer","description":"Look back this many days (1–90). Use 30 for recent history."},"limit":{"type":"integer","description":"Maximum number of sessions to return (1–20). Use 5 for a brief summary."}},"required":["days","limit"],"additionalProperties":false},"strict":true}"""

    private const val SPEC_SYNC_STATUS =
        """{"type":"function","name":"get_sync_status","description":"Returns the current ring connection state and the timestamp of the last successful data sync.","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_DATA_AVAILABILITY =
        """{"type":"function","name":"get_data_availability","description":"Returns a summary of what health data is available in the database (counts and date ranges for each metric).","parameters":$NO_PARAMS,"strict":true}"""

    private const val SPEC_SLEEP_TRENDS =
        """{"type":"function","name":"get_sleep_trends","description":"Returns sleep session data for the last N nights including total duration, stage breakdown, and sleep scores.","parameters":{"type":"object","properties":{"nights":{"type":"integer","description":"Number of nights to retrieve (1–30). Use 7 for a weekly trend."}},"required":["nights"],"additionalProperties":false},"strict":true}"""

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
            val days    = JSONObject(args).optInt("days", 7).coerceIn(1, 90)
            val cutoff  = cutoffMs(days)
            val rows    = ctx.activityDailyDao.getSince(cutoff)
            val hrRows  = ctx.measurementDao.getByKindSince("hr", cutoff)
            val spo2Rows = ctx.measurementDao.getByKindSince("spo2", cutoff)
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
                put("days_requested",    days)
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
            val days   = o.optInt("days", 7).coerceIn(1, 90)
            val cutoff = cutoffMs(days)

            val arr = JSONArray()
            when (metric) {
                "hr", "spo2", "hrv", "stress", "temp" -> {
                    val kind = if (metric == "temp") "temp" else metric
                    val rows = downsample(ctx.measurementDao.getByKindSince(kind, cutoff).sortedBy { it.timestamp })
                    for (r in rows) {
                        arr.put(JSONObject().put("ts", isoString(r.timestamp)).put("v", r.value))
                    }
                }
                "steps", "calories", "distance", "active_minutes" -> {
                    val rows    = ctx.activityDailyDao.getSince(cutoff).sortedBy { it.date }
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
                put("days",   days)
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
            val days  = o.optInt("days", 30).coerceIn(1, 90)
            val limit = o.optInt("limit", 5).coerceIn(1, 20)
            val cutoff = cutoffMs(days)

            val sessions = ctx.activitySessionDao.getAll()
                .filter { it.startedAt >= cutoff && it.statusRaw == "finished" }
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
                })
            }
            ToolResult.success(JSONObject().put("sessions", arr).put("count", arr.length()))
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

    private fun getDataAvailability() = CoachTool(
        name = "get_data_availability",
        specJson = SPEC_DATA_AVAILABILITY,
        run = { _, ctx ->
            val hrCount    = ctx.measurementDao.getByKind("hr").size
            val spo2Count  = ctx.measurementDao.getByKind("spo2").size
            val hrvCount   = ctx.measurementDao.getByKind("hrv").size
            val stressCount = ctx.measurementDao.getByKind("stress").size
            val tempCount  = ctx.measurementDao.getByKind("temp").size
            val actDays    = ctx.activityDailyDao.getAll().size
            val sleepNights = ctx.sleepDao.getAllSessions().size

            val obj = JSONObject().apply {
                put("heart_rate_readings", hrCount)
                put("spo2_readings",       spo2Count)
                put("hrv_readings",        hrvCount)
                put("stress_readings",     stressCount)
                put("temperature_readings", tempCount)
                put("activity_days",       actDays)
                put("sleep_nights",        sleepNights)
            }
            ToolResult.success(obj)
        }
    )

    private fun getSleepTrends() = CoachTool(
        name = "get_sleep_trends",
        specJson = SPEC_SLEEP_TRENDS,
        run = { args, ctx ->
            val nights = JSONObject(args).optInt("nights", 7).coerceIn(1, 30)
            val cutoff = cutoffMs(nights)

            val sessions = ctx.sleepDao.getAllSessions()
                .filter { it.startAt >= cutoff }
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
                put("nights_requested", nights)
                put("nights_available", sessions.size)
                put("sessions",         arr)
                put("avg_duration_min", if (totalMins.isEmpty()) null else totalMins.average())
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
