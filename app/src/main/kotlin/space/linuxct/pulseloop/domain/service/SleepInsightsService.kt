package space.linuxct.pulseloop.domain.service

import space.linuxct.pulseloop.domain.model.SleepBar
import space.linuxct.pulseloop.domain.model.SleepRangeKey
import space.linuxct.pulseloop.domain.model.SleepSummary
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.sqrt

// ─── Score ────────────────────────────────────────────────────────────────────

enum class SleepQualityLabel(val label: String) {
    EXCELLENT("Excellent"), GOOD("Good"), FAIR("Fair"), NEEDS_WORK("Needs work")
}

data class SleepScoreResult(
    val score: Int,
    val label: SleepQualityLabel,
    val deepPct: Int,
    val lightPct: Int,
    val awakePct: Int?
)

object SleepScore {

    private fun clamp(v: Double, lo: Double, hi: Double) = minOf(hi, maxOf(lo, v))

    private fun bandScore(
        value: Double,
        idealLow: Double, idealHigh: Double,
        softLow: Double, softHigh: Double,
        hardLow: Double, hardHigh: Double,
        points: Double
    ): Double {
        if (!value.isFinite()) return 0.0
        if (value in idealLow..idealHigh) return points
        if (value < idealLow && value >= softLow)
            return points * (0.65 + 0.35 * ((value - softLow) / (idealLow - softLow)))
        if (value > idealHigh && value <= softHigh)
            return points * (0.65 + 0.35 * ((softHigh - value) / (softHigh - idealHigh)))
        if (value < softLow)
            return points * 0.65 * clamp((value - hardLow) / (softLow - hardLow), 0.0, 1.0)
        return points * 0.65 * clamp((hardHigh - value) / (hardHigh - softHigh), 0.0, 1.0)
    }

    private fun awakeScore(awakePct: Double?, points: Double): Double {
        if (awakePct == null || !awakePct.isFinite()) return points * 0.55
        if (awakePct <= 10) return points
        if (awakePct <= 20) return points * (1 - 0.65 * ((awakePct - 10) / 10))
        return points * 0.35 * clamp((35 - awakePct) / 15, 0.0, 1.0)
    }

    fun qualityLabel(score: Int): SleepQualityLabel = when {
        score >= 85 -> SleepQualityLabel.EXCELLENT
        score >= 70 -> SleepQualityLabel.GOOD
        score >= 55 -> SleepQualityLabel.FAIR
        else        -> SleepQualityLabel.NEEDS_WORK
    }

    fun calculate(sleep: SleepSummary): SleepScoreResult {
        val totalMinutes = maxOf(0, sleep.totalMinutes)
        val total  = totalMinutes.toDouble()
        val deep   = maxOf(0, sleep.deepMinutes).toDouble()
        val light  = maxOf(0, sleep.lightMinutes).toDouble()
        val awake  = maxOf(0, sleep.awakeMinutes).toDouble()
        val coveredStageMin = sleep.blocks.sumOf { b ->
            when (b.stageRaw) {
                "deep", "light", "awake" -> maxOf(0, b.durationMinutes)
                else -> 0
            }
        }.toDouble()
        val hasAwakeSignal = sleep.blocks.any { it.stageRaw == "awake" } ||
            awake > 0 || (total > 0 && coveredStageMin >= total * 0.95)

        val totalHours = total / 60
        val deepPct  = if (total > 0) (deep / total) * 100 else 0.0
        val lightPct = if (total > 0) (light / total) * 100 else 0.0
        val awakePct: Double? = if (total > 0 && hasAwakeSignal) (awake / total) * 100 else null

        val duration  = bandScore(totalHours, 7.5, 8.5, 6.0, 9.5, 3.0, 12.0, 35.0)
        val deepScore = bandScore(deepPct,    13.0, 23.0, 5.0, 35.0, 0.0, 45.0, 30.0)
        val lightScore = bandScore(lightPct,  50.0, 60.0, 35.0, 75.0, 20.0, 90.0, 20.0)
        val awakeSub  = awakeScore(awakePct, 15.0)
        val score = clamp((duration + deepScore + lightScore + awakeSub).let { kotlin.math.round(it).toDouble() }, 0.0, 100.0).toInt()

        return SleepScoreResult(
            score = score,
            label = qualityLabel(score),
            deepPct  = deepPct.let { kotlin.math.round(it).toInt() },
            lightPct = lightPct.let { kotlin.math.round(it).toInt() },
            awakePct = awakePct?.let { kotlin.math.round(it).toInt() }
        )
    }
}

// ─── Coach text ───────────────────────────────────────────────────────────────

data class SleepCoach(val headline: String, val body: String, val chips: List<String>)
data class SleepNoDataState(val label: String, val value: String, val support: String)

object SleepInsights {

    val rangeHeroLabel = mapOf(
        SleepRangeKey.DAY   to "Last Sleep",
        SleepRangeKey.WEEK  to "Weekly Sleep",
        SleepRangeKey.MONTH to "Monthly Sleep",
        SleepRangeKey.YEAR  to "Yearly Sleep"
    )

    private const val MIN_AGG_NIGHTS = 2

    fun validSessions(sessions: List<SleepSummary>): List<SleepSummary> {
        return sessions.filter { s -> s.totalMinutes > 0 }
    }

    fun averageDuration(valid: List<SleepSummary>): Int? {
        if (valid.isEmpty()) return null
        return valid.sumOf { it.totalMinutes } / valid.count()
    }

    fun averageScore(valid: List<SleepSummary>): Int? {
        if (valid.isEmpty()) return null
        val total = valid.sumOf { SleepScore.calculate(it).score }
        return (total.toDouble() / valid.count()).let { kotlin.math.round(it).toInt() }
    }

    fun averageStages(valid: List<SleepSummary>): Triple<Int, Int, Int>? {
        if (valid.isEmpty()) return null
        return Triple(
            valid.sumOf { it.deepMinutes } / valid.count(),
            valid.sumOf { it.lightMinutes } / valid.count(),
            valid.sumOf { it.awakeMinutes } / valid.count()
        )
    }

    private fun durationConsistency(valid: List<SleepSummary>): Double? {
        if (valid.size < 2) return null
        val avg = averageDuration(valid)?.toDouble() ?: return null
        val variance = valid.sumOf { s ->
            val d = (s.totalMinutes - avg).pow(2)
            d
        } / valid.count()
        return sqrt(variance)
    }

    private fun nightsTrackedChip(valid: Int, expected: Int) = "$valid of $expected tracked"
    private fun goalDeltaChip(avgMin: Int, goalMin: Int?): String? {
        if (goalMin == null) return null
        val delta = avgMin - goalMin
        if (kotlin.math.abs(delta) <= 20) return "On target"
        return if (delta < 0) "Below goal" else "Above goal"
    }
    private fun consistencyChip(valid: List<SleepSummary>): String? {
        val sd = durationConsistency(valid) ?: return null
        return when {
            sd <= 40 -> "Consistent"
            sd >= 80 -> "Variable nights"
            else     -> null
        }
    }

    fun dayCoach(sleep: SleepSummary, score: Int, awakePct: Int?, deepPct: Int, activitySteps: Int?): SleepCoach {
        val totalMin = sleep.totalMinutes
        var chips = mutableListOf<String>()
        if (totalMin in 420..540) chips.add("Good duration")
        if (deepPct in 13..23) chips.add("Deep sleep balanced")
        else if (deepPct > 23) chips.add("Deep sleep strong")
        if (awakePct != null && awakePct <= 10) chips.add("Awake time low")

        if (score >= 85) {
            val body = if ((activitySteps ?: 0) > 5000)
                "Looks like a strong night after a more active day. Your duration was solid and deep sleep made up a healthy part of the night, which kept the score high."
            else
                "Your sleep duration and stage balance were strong. Deep sleep looked supportive, which helped keep the overall score high."
            return SleepCoach("Strong recovery signal", body, (if (chips.isEmpty()) listOf("Excellent") else chips).take(3))
        }
        if (awakePct != null && awakePct > 15) {
            return SleepCoach(
                "Good sleep, with some restlessness",
                "You slept long enough, but awake time was a bit elevated. If this repeats, look at late caffeine, alcohol, temperature, or stress near bedtime.",
                (listOf("Awake time elevated") + chips).take(3)
            )
        }
        if (totalMin < 390) {
            return SleepCoach(
                "Duration held the score back",
                "The stage mix was useful, but total sleep time was short for a full recovery window. A slightly earlier wind-down would likely improve tomorrow's score.",
                (listOf("Short duration") + chips).take(3)
            )
        }
        return SleepCoach(
            "Solid night overall",
            "Your sleep was in a workable range, with the score shaped mostly by duration and stage balance. Deep and light sleep were readable enough to give a useful recovery snapshot.",
            (if (chips.isEmpty()) listOf("Good") else chips).take(3)
        )
    }

    val dayNoDataCoach = SleepCoach(
        "No sleep tracked last night",
        "I don't see sleep data for last night. Wear your ring overnight and sync in the morning so I can compare your sleep against your baseline.",
        emptyList()
    )

    fun aggregateCoach(range: SleepRangeKey, sessions: List<SleepSummary>, expectedNights: Int, goalMin: Int?): SleepCoach {
        val valid = validSessions(sessions)
        val avgMin = averageDuration(valid)
        val periodWord = when (range) { SleepRangeKey.WEEK -> "week"; SleepRangeKey.MONTH -> "month"; else -> "year" }

        if (valid.size < MIN_AGG_NIGHTS) {
            val nightWord = if (valid.size == 1) "night" else "nights"
            return SleepCoach(
                "Not enough $periodWord data yet",
                "I only have ${valid.size} tracked $nightWord for this $periodWord. Wear the ring overnight for a few more nights and I'll build a reliable ${periodWord}ly picture.",
                listOf(nightsTrackedChip(valid.size, expectedNights))
            )
        }

        var chips = mutableListOf(nightsTrackedChip(valid.size, expectedNights))
        if (avgMin != null) goalDeltaChip(avgMin, goalMin)?.let { chips.add(it) }
        consistencyChip(valid)?.let { chips.add(it) }
        chips = chips.take(3).toMutableList()

        val avgText = sleepFormatDuration(avgMin)
        val coveragePhrase = "${valid.size} of $expectedNights nights tracked"

        return when (range) {
            SleepRangeKey.WEEK -> {
                val missing = expectedNights - valid.size
                val missingWord = if (missing == 1) "night" else "nights"
                val body = if (valid.size < expectedNights)
                    "You averaged $avgText across ${valid.size} tracked nights this week. That's a useful read, but $missing missing $missingWord mean the trend is still incomplete."
                else
                    "You averaged $avgText across the full week. Your nights were tracked consistently, so this is a dependable picture of where your sleep sits right now."
                SleepCoach("Your week at a glance", body, chips)
            }
            SleepRangeKey.MONTH -> {
                val sparse = valid.size.toDouble() < expectedNights * 0.5
                val body = if (sparse)
                    "Your monthly average is $avgText, but coverage is low ($coveragePhrase), so I'd treat that number cautiously. More nights tracked will sharpen the trend."
                else
                    "Your monthly average is $avgText across $coveragePhrase. The biggest lever is consistency — a few short nights move this number more than any single great one."
                SleepCoach("Your month in sleep", body, chips)
            }
            else -> SleepCoach(
                "Your long-term sleep trend",
                "Across the year your tracked average is $avgText over ${valid.size} nights. The long-term trend is still forming — as more months fill in, I'll be able to compare seasonal changes and consistency.",
                chips
            )
        }
    }

    fun noDataState(range: SleepRangeKey): SleepNoDataState = when (range) {
        SleepRangeKey.DAY   -> SleepNoDataState("Last Sleep", "No sleep captured last night", "Wear your ring overnight so PulseLoop can track your next night.")
        SleepRangeKey.WEEK  -> SleepNoDataState("Weekly Sleep", "Not enough weekly data", "Wear your ring overnight for a few nights to build a weekly view.")
        SleepRangeKey.MONTH -> SleepNoDataState("Monthly Sleep", "Not enough monthly data", "Track more nights this month to see a monthly average.")
        SleepRangeKey.YEAR  -> SleepNoDataState("Yearly Sleep", "Not enough yearly data", "Long-term insights appear as more nights are tracked.")
    }

    fun buildNightAxis(startMs: Long, endMs: Long, sessions: List<SleepSummary>, range: SleepRangeKey): List<SleepBar> {
        val cal = Calendar.getInstance()
        val byDate = sessions.associateBy { s ->
            cal.timeInMillis = s.session.startAt
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val bars = mutableListOf<SleepBar>()
        cal.timeInMillis = startMs
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val lastMs = run { val c2 = Calendar.getInstance(); c2.timeInMillis = endMs; c2.set(Calendar.HOUR_OF_DAY, 0); c2.set(Calendar.MINUTE, 0); c2.set(Calendar.SECOND, 0); c2.set(Calendar.MILLISECOND, 0); c2.timeInMillis }
        while (cal.timeInMillis <= lastMs) {
            val session = byDate[cal.timeInMillis]
            val totalMin = session?.totalMinutes ?: 0
            val present = totalMin > 0
            val label = if (range == SleepRangeKey.WEEK) {
                arrayOf("S","M","T","W","T","F","S")[cal.get(Calendar.DAY_OF_WEEK) - 1]
            } else {
                cal.get(Calendar.DAY_OF_MONTH).toString()
            }
            bars.add(SleepBar(label = label, durationMin = if (present) totalMin else null, score = if (present) session?.let { SleepScore.calculate(it).score } else null, present = present))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return bars
    }

    fun buildMonthBuckets(endMs: Long, sessions: List<SleepSummary>): List<SleepBar> {
        val valid = validSessions(sessions)
        val byMonth = mutableMapOf<String, MutableList<SleepSummary>>()
        val cal = Calendar.getInstance()
        for (s in valid) {
            cal.timeInMillis = s.session.startAt
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            byMonth.getOrPut(key) { mutableListOf() }.add(s)
        }
        val bars = mutableListOf<SleepBar>()
        cal.timeInMillis = endMs
        for (i in 11 downTo 0) {
            val c2 = Calendar.getInstance().also { it.timeInMillis = endMs; it.add(Calendar.MONTH, -i) }
            val key = "${c2.get(Calendar.YEAR)}-${c2.get(Calendar.MONTH)}"
            val monthSessions = byMonth[key] ?: emptyList()
            val monthAbbrev = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(c2.time)
            bars.add(SleepBar(
                label = monthAbbrev,
                durationMin = averageDuration(monthSessions),
                score = averageScore(monthSessions),
                present = monthSessions.isNotEmpty()
            ))
        }
        return bars
    }

    private fun sleepFormatDuration(minutes: Int?): String {
        if (minutes == null || minutes < 0) return "—"
        val h = minutes / 60; val m = minutes % 60
        return if (h <= 0) "${m}m" else "${h}h ${"%02d".format(m)}m"
    }
}
