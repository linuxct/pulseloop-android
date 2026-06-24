package space.linuxct.pulseloop.ble.colmi

import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.SleepStage
import java.util.Calendar

class ColmiDecoder {

    fun decodeNormal(data: ByteArray, nowMs: Long = System.currentTimeMillis()): List<RingDecodedEvent> {
        val packet = ColmiPacket.validate(data) ?: return listOf(
            RingDecodedEvent.Unknown(data.firstOrNull() ?: 0, data)
        )
        val v = packet.bytes
        return when (v[0]) {
            ColmiCommandId.BATTERY -> listOf(RingDecodedEvent.Battery(v[1].toInt() and 0xFF))
            ColmiCommandId.MANUAL_HEART_RATE -> {
                val errorCode = v[2].toInt() and 0xFF
                val bpm = v[3].toInt() and 0xFF
                if (errorCode != 0) {
                    listOf(RingDecodedEvent.HeartRateComplete(nowMs))
                } else if (bpm !in 30..220) {
                    emptyList() // warm-up zero or noise
                } else {
                    listOf(RingDecodedEvent.HeartRateSample(bpm, nowMs))
                }
            }
            ColmiCommandId.REALTIME_HEART_RATE -> {
                val bpm = v[1].toInt() and 0xFF
                if (bpm in 30..220) listOf(RingDecodedEvent.HeartRateSample(bpm, nowMs)) else emptyList()
            }
            ColmiCommandId.REALTIME_HEART_RATE_ERROR -> listOf(RingDecodedEvent.HeartRateComplete(nowMs))
            ColmiCommandId.NOTIFICATION -> decodeNotification(v, nowMs)
            else -> listOf(RingDecodedEvent.CommandAck(v[0]))
        }
    }

    fun decodeHistory(data: ByteArray, dayMidnightMs: Long): List<RingDecodedEvent> {
        val packet = ColmiPacket.validate(data) ?: return emptyList()
        val v = packet.bytes
        return when (v[0]) {
            ColmiCommandId.SYNC_HEART_RATE  -> decodeHrHistory(v, dayMidnightMs)
            ColmiCommandId.SYNC_STRESS      -> decodeStressHistory(v, dayMidnightMs)
            ColmiCommandId.SYNC_HRV         -> decodeHrvHistory(v, dayMidnightMs)
            ColmiCommandId.SYNC_ACTIVITY    -> decodeActivityHistory(v)
            else -> emptyList()
        }
    }

    fun decodeBigData(data: ByteArray, nowMs: Long = System.currentTimeMillis()): List<RingDecodedEvent> {
        if (data.size < 6 || data[0] != ColmiCommandId.BIG_DATA_V2) {
            return listOf(RingDecodedEvent.Unknown(data.firstOrNull() ?: 0, data))
        }
        return when (data[1]) {
            ColmiCommandId.BIG_DATA_SPO2        -> decodeSpo2(data)
            ColmiCommandId.BIG_DATA_SLEEP       -> decodeSleep(data)
            ColmiCommandId.BIG_DATA_TEMPERATURE -> decodeTemperature(data)
            else -> listOf(RingDecodedEvent.Unknown(data[1], data))
        }
    }

    companion object {
        fun historyPacketNumber(data: ByteArray): Int? {
            if (data.size < 2) return null
            return data[1].toInt() and 0xFF
        }

        fun sleepStage(type: Byte): SleepStage = when (type) {
            ColmiCommandId.SLEEP_LIGHT -> SleepStage.LIGHT
            ColmiCommandId.SLEEP_DEEP  -> SleepStage.DEEP
            ColmiCommandId.SLEEP_REM   -> SleepStage.REM
            ColmiCommandId.SLEEP_AWAKE -> SleepStage.AWAKE
            else                       -> SleepStage.UNKNOWN
        }
    }

    private fun decodeNotification(v: ByteArray, nowMs: Long): List<RingDecodedEvent> {
        return when (v[1]) {
            ColmiCommandId.NOTIF_BATTERY ->
                listOf(RingDecodedEvent.Battery(v[2].toInt() and 0xFF))
            ColmiCommandId.NOTIF_LIVE_ACTIVITY -> {
                val steps    = ColmiBytes.u24(v[2], v[3], v[4])
                val calories = ColmiBytes.u24(v[5], v[6], v[7]).toDouble() / 10.0
                val distance = ColmiBytes.u24(v[8], v[9], v[10]).toDouble()
                listOf(RingDecodedEvent.ActivityUpdate(nowMs, steps, distance, calories))
            }
            else -> listOf(RingDecodedEvent.CommandAck(v[0]))
        }
    }

    private fun decodeHrHistory(v: ByteArray, dayMs: Long): List<RingDecodedEvent> {
        val packetNr = v[1].toInt() and 0xFF
        if (packetNr == 0xFF || packetNr == 0) return emptyList()
        val startIndex = if (packetNr == 1) 6 else 2
        val minutesPrior = when {
            packetNr <= 1 -> 0
            else          -> 9 * 5 + (packetNr - 2) * 13 * 5
        }
        val events = mutableListOf<RingDecodedEvent>()
        for (i in startIndex until v.size - 1) {
            val bpm = v[i].toInt() and 0xFF
            if (bpm == 0) continue
            val minuteOfDay = minutesPrior + (i - startIndex) * 5
            events.add(RingDecodedEvent.HistoryMeasurement(MeasurementKind.HEART_RATE, bpm.toDouble(), dayMs + minuteOfDay * 60_000L))
        }
        return events
    }

    private fun decodeStressHistory(v: ByteArray, dayMs: Long): List<RingDecodedEvent> {
        val packetNr = v[1].toInt() and 0xFF
        if (packetNr == 0xFF || packetNr == 0) return emptyList()
        val startIndex = if (packetNr == 1) 3 else 2
        val minutesPrior = when {
            packetNr <= 1 -> 0
            else          -> 12 * 30 + (packetNr - 2) * 13 * 30
        }
        val events = mutableListOf<RingDecodedEvent>()
        for (i in startIndex until v.size - 1) {
            val stress = v[i].toInt() and 0xFF
            if (stress == 0) continue
            val minuteOfDay = minutesPrior + (i - startIndex) * 30
            events.add(RingDecodedEvent.StressSample(stress, dayMs + minuteOfDay * 60_000L))
        }
        return events
    }

    private fun decodeHrvHistory(v: ByteArray, dayMs: Long): List<RingDecodedEvent> {
        val packetNr = v[1].toInt() and 0xFF
        if (packetNr == 0xFF || packetNr == 0) return emptyList()
        val startIndex = if (packetNr == 1) 3 else 2
        val minutesPrior = when {
            packetNr <= 1 -> 0
            else          -> 12 * 30 + (packetNr - 2) * 13 * 30
        }
        val events = mutableListOf<RingDecodedEvent>()
        for (i in startIndex until v.size - 1) {
            val hrv = v[i].toInt() and 0xFF
            if (hrv == 0) continue
            val minuteOfDay = minutesPrior + (i - startIndex) * 30
            events.add(RingDecodedEvent.HrvSample(hrv, dayMs + minuteOfDay * 60_000L))
        }
        return events
    }

    private fun decodeActivityHistory(v: ByteArray): List<RingDecodedEvent> {
        val marker = v[1].toInt() and 0xFF
        if (marker == 0xFF || marker == 0xF0 || v.size < 13) return emptyList()

        fun bcdLit(b: Byte): Int = Integer.parseInt("%02x".format(b.toInt() and 0xFF))
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2000 + bcdLit(v[1]))
        cal.set(Calendar.MONTH, bcdLit(v[2]) - 1)
        cal.set(Calendar.DAY_OF_MONTH, bcdLit(v[3]))
        cal.set(Calendar.HOUR_OF_DAY, minOf(23, maxOf(0, (v[4].toInt() and 0xFF) / 4)))
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val ts = cal.timeInMillis

        val nowMs = System.currentTimeMillis()
        val lowerBound = nowMs - 8 * 86_400_000L
        val upperBound = nowMs + 3_600_000L
        if (ts < lowerBound || ts > upperBound) return emptyList()

        val steps    = ColmiBytes.u16(v[9], v[10])
        val distance = ColmiBytes.u16(v[11], v[12]).toDouble()
        return listOf(RingDecodedEvent.ActivityBucket(ts, steps, distance))
    }

    private fun decodeSpo2(data: ByteArray): List<RingDecodedEvent> {
        val length = ColmiBytes.u16(data[2], data[3])
        var index = 6
        val events = mutableListOf<RingDecodedEvent>()
        var daysAgo = -1
        while (daysAgo != 0 && index - 6 < length && index < data.size) {
            daysAgo = data[index].toInt() and 0xFF
            index++
            val dayStartMs = dayMidnightMs(daysAgo)
            for (hour in 0..23) {
                if (index + 1 >= data.size) break
                val lo = data[index].toInt() and 0xFF; index++
                val hi = data[index].toInt() and 0xFF; index++
                if (lo > 0 && hi > 0) {
                    val value = ((lo + hi) / 2.0).let { kotlin.math.round(it).toDouble() }
                    events.add(RingDecodedEvent.HistoryMeasurement(MeasurementKind.SPO2, value, dayStartMs + hour * 3_600_000L))
                }
                if (index - 6 >= length) break
            }
        }
        return events
    }

    private fun decodeTemperature(data: ByteArray): List<RingDecodedEvent> {
        val length = ColmiBytes.u16(data[2], data[3])
        if (length < 50) return emptyList()
        var index = 6
        val events = mutableListOf<RingDecodedEvent>()
        var daysAgo = -1
        while (daysAgo != 0 && index - 6 < length && index < data.size) {
            daysAgo = data[index].toInt() and 0xFF; index++
            index++ // skip unknown byte (observed 0x1e)
            val dayStartMs = dayMidnightMs(daysAgo)
            for (hour in 0..23) {
                if (index + 1 >= data.size) break
                val t00 = data[index].toInt() and 0xFF; index++
                val t30 = data[index].toInt() and 0xFF; index++
                if (t00 > 0) events.add(RingDecodedEvent.TemperatureSample(t00 / 10.0 + 20.0, dayStartMs + hour * 3_600_000L))
                if (t30 > 0) events.add(RingDecodedEvent.TemperatureSample(t30 / 10.0 + 20.0, dayStartMs + hour * 3_600_000L + 1_800_000L))
                if (index - 6 >= length) break
            }
        }
        return events
    }

    private fun decodeSleep(data: ByteArray): List<RingDecodedEvent> {
        val packetLength = ColmiBytes.u16(data[2], data[3])
        if (packetLength < 2 || data.size <= 7) return emptyList()
        val daysInPacket = data[6].toInt() and 0xFF
        var index = 7
        val events = mutableListOf<RingDecodedEvent>()
        for (dayIdx in 0 until daysInPacket) {
            if (index + 5 >= data.size) break
            val daysAgo       = data[index].toInt() and 0xFF; index++
            val dayBytes      = data[index].toInt() and 0xFF; index++
            val sleepStart    = ColmiBytes.u16(data[index], data[index + 1]); index += 2
            val sleepEnd      = ColmiBytes.u16(data[index], data[index + 1]); index += 2
            val dayStartMs    = dayMidnightMs(daysAgo)
            val startOffset   = if (sleepStart > sleepEnd) sleepStart - 1440 else sleepStart
            val sessionStartMs = dayStartMs + startOffset * 60_000L

            val stages = mutableListOf<SleepStage>()
            var j = 4
            while (j < dayBytes && index + 1 < data.size) {
                val stageType = data[index]; index++
                val minutes   = data[index].toInt() and 0xFF; index++
                j += 2
                if (minutes <= 0) continue
                val stage = sleepStage(stageType)
                repeat(minutes) { stages.add(stage) }
            }
            if (stages.isNotEmpty()) {
                events.add(RingDecodedEvent.SleepTimeline(sessionStartMs, stages))
            }
        }
        return events
    }

    private fun dayMidnightMs(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
