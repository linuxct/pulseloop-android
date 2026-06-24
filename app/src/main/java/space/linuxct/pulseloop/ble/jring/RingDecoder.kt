package space.linuxct.pulseloop.ble.jring

import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.core.util.getUInt8
import space.linuxct.pulseloop.core.util.getUInt32Le
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.SleepStage

class RingDecoder {

    fun decode(data: ByteArray): RingDecodedEvent {
        if (data.size != 20) return RingDecodedEvent.Unknown(data.firstOrNull() ?: 0, data)
        val now = System.currentTimeMillis()
        return when (data[0]) {
            JringCommandId.TIME_SYNC -> {
                if (data.size >= 5) {
                    RingDecodedEvent.TimeSyncAck(timestamp = getU32Le(data, 1) * 1000L)
                } else RingDecodedEvent.CommandAck(data[0])
            }
            JringCommandId.ACTIVITY_QUERY_ACK -> {
                RingDecodedEvent.CommandAck(data[0])
            }
            JringCommandId.CURRENT_ACTIVITY -> {
                if (data.size >= 17) {
                    val timestamp = getU32Le(data, 1) * 1000L
                    val steps = getU32Le(data, 5).toInt()
                    val distance = getU32Le(data, 9).toDouble()
                    val calories = getU32Le(data, 13).toDouble()
                    RingDecodedEvent.ActivityUpdate(timestamp, steps, distance, calories)
                } else RingDecodedEvent.Unknown(data[0], data)
            }
            JringCommandId.PERCENT_STATUS -> {
                if (data.size >= 2) RingDecodedEvent.Battery(data.getUInt8(1))
                else RingDecodedEvent.Unknown(data[0], data)
            }
            JringCommandId.STATUS -> {
                val address = if (data.size >= 9) {
                    data.slice(3..8).joinToString(":") { "%02x".format(it) }
                } else null
                RingDecodedEvent.Status(address)
            }
            JringCommandId.SLEEP_TIMELINE -> {
                if (data.size >= 20) {
                    val timestamp = getU32Le(data, 1) * 1000L
                    val stages = (5 until 20).map { toSleepStage(data[it]) }
                    RingDecodedEvent.SleepTimeline(timestamp, stages)
                } else RingDecodedEvent.Unknown(data[0], data)
            }
            JringCommandId.HEART_RATE -> {
                if (data.size >= 6) RingDecodedEvent.HeartRateSample(data.getUInt8(5), now)
                else RingDecodedEvent.Unknown(data[0], data)
            }
            JringCommandId.HISTORY_MEASUREMENT -> {
                if (data.size >= 9) {
                    val timestamp = if (data[1] == 0xaa.toByte()) {
                        getU32Le(data, 3) * 1000L
                    } else {
                        getU32Le(data, 2) * 1000L
                    }
                    val values = data.drop(8).filter { it.toInt() and 0xFF > 0 }
                    val first = values.firstOrNull()
                    if (first != null) {
                        RingDecodedEvent.HistoryMeasurement(
                            kind = MeasurementKind.HEART_RATE,
                            value = (first.toInt() and 0xFF).toDouble(),
                            timestamp = timestamp
                        )
                    } else {
                        RingDecodedEvent.CommandAck(data[0])
                    }
                } else RingDecodedEvent.Unknown(data[0], data)
            }
            JringCommandId.SPO2_RESULT_PROGRESS -> {
                if (data.size >= 5 && (80..100).contains(data.getUInt8(4))) {
                    RingDecodedEvent.Spo2Result(data.getUInt8(4), now)
                } else {
                    RingDecodedEvent.Spo2Progress(null, now)
                }
            }
            JringCommandId.HEART_RATE_COMPLETE -> RingDecodedEvent.HeartRateComplete(now)
            JringCommandId.SPO2_COMPLETE       -> RingDecodedEvent.Spo2Complete(now)
            else -> RingDecodedEvent.Unknown(data[0], data)
        }
    }

    private fun getU32Le(data: ByteArray, offset: Int): Long = data.getUInt32Le(offset)

    private fun toSleepStage(byte: Byte): SleepStage = when (byte.toInt() and 0xFF) {
        0x28 -> SleepStage.LIGHT
        0x63 -> SleepStage.DEEP
        0x00 -> SleepStage.AWAKE
        else -> SleepStage.UNKNOWN
    }
}
