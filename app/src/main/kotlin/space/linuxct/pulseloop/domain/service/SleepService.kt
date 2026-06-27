package space.linuxct.pulseloop.domain.service

import space.linuxct.pulseloop.data.db.entities.SleepSessionEntity
import space.linuxct.pulseloop.data.db.entities.SleepStageBlockEntity
import space.linuxct.pulseloop.domain.model.SleepRangeKey
import space.linuxct.pulseloop.domain.model.SleepSummary
import space.linuxct.pulseloop.domain.model.SleepRangeSummary
import java.util.Calendar

object SleepService {

    fun latestSleep(sessions: List<SleepSessionEntity>, blocks: Map<String, List<SleepStageBlockEntity>>): SleepSummary? {
        val session = sessions.maxByOrNull { it.startAt } ?: return null
        val staleCutoff = todayMidnightMs() - 86_400_000L
        if (dayMidnightMs(session.startAt) < staleCutoff) return null
        return summary(session, blocks[session.id] ?: emptyList(), includeStages = true)
    }

    fun sleepForDate(dateMs: Long, sessions: List<SleepSessionEntity>, blocks: Map<String, List<SleepStageBlockEntity>>): SleepSummary? {
        val dayMs = dayMidnightMs(dateMs)
        val session = sessions.firstOrNull { dayMidnightMs(it.startAt) == dayMs } ?: return null
        return summary(session, blocks[session.id] ?: emptyList(), includeStages = true)
    }

    fun sleepRange(range: SleepRangeKey, sessions: List<SleepSessionEntity>, blocks: Map<String, List<SleepStageBlockEntity>>): SleepRangeSummary {
        val expected = expectedNights(range)
        val anchor = if (range == SleepRangeKey.DAY) dayReferenceNight() else (sessions.maxByOrNull { it.startAt }?.let { dayMidnightMs(it.startAt) } ?: todayMidnightMs())
        val startMs = anchor - (expected - 1) * 86_400_000L
        val endMs   = anchor + 86_400_000L - 1
        val includeStages = range == SleepRangeKey.DAY
        val summaries = sessions.filter { it.startAt in startMs..endMs }.map { session ->
            summary(session, blocks[session.id] ?: emptyList(), includeStages)
        }
        return SleepRangeSummary(range = range, start = startMs, end = anchor, expectedNights = expected, sessions = summaries)
    }

    fun dayReferenceNight(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val todayMidnight = todayMidnightMs()
        return if (hour < 4) todayMidnight - 86_400_000L else todayMidnight
    }

    fun summary(session: SleepSessionEntity, blocks: List<SleepStageBlockEntity>, includeStages: Boolean = true): SleepSummary {
        val filled = withFilledGaps(session, blocks)
        val light = filled.filter { it.stageRaw == "light" }.sumOf { it.durationMinutes }
        val deep  = filled.filter { it.stageRaw == "deep" }.sumOf { it.durationMinutes }
        val awake = filled.filter { it.stageRaw == "awake" }.sumOf { it.durationMinutes }
        val rem   = filled.filter { it.stageRaw == "rem" }.sumOf { it.durationMinutes }
        return SleepSummary(
            session      = session,
            blocks       = if (includeStages) filled else emptyList(),
            lightMinutes = light,
            deepMinutes  = deep,
            awakeMinutes = awake,
            remMinutes   = rem
        )
    }

    // The jring omits 15-minute windows from its sleep history dump when the person was awake
    // during that period — it only sends packets for LIGHT and DEEP windows. Gaps between
    // consecutive blocks are therefore brief awakenings; fill them so the chart and stats
    // reflect reality instead of showing silent holes.
    private fun withFilledGaps(session: SleepSessionEntity, blocks: List<SleepStageBlockEntity>): List<SleepStageBlockEntity> {
        if (blocks.isEmpty()) return blocks
        val sorted = blocks.sortedBy { it.startAt }
        val result = mutableListOf<SleepStageBlockEntity>()
        for (i in sorted.indices) {
            result.add(sorted[i])
            if (i + 1 < sorted.size) {
                val blockEndMs  = sorted[i].startAt + sorted[i].durationMinutes * 60_000L
                val nextStartMs = sorted[i + 1].startAt
                val gapMinutes  = ((nextStartMs - blockEndMs) / 60_000L).toInt()
                if (gapMinutes > 0) {
                    result.add(SleepStageBlockEntity(
                        id              = "synth_awake_$blockEndMs",
                        sessionId       = session.id,
                        stageRaw        = "awake",
                        startAt         = blockEndMs,
                        durationMinutes = gapMinutes
                    ))
                }
            }
        }
        return result
    }

    private fun expectedNights(range: SleepRangeKey): Int = when (range) {
        SleepRangeKey.DAY   -> 1
        SleepRangeKey.WEEK  -> 7
        SleepRangeKey.MONTH -> 30
        SleepRangeKey.YEAR  -> 365
    }

    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayMidnightMs(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
