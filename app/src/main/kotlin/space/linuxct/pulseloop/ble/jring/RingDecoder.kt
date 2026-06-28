package space.linuxct.pulseloop.ble.jring

import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.core.util.getUInt8
import space.linuxct.pulseloop.core.util.getUInt32Le
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.SleepStage

class RingDecoder {

    fun decode(data: ByteArray): List<RingDecodedEvent> {
        if (data.size != 20) return listOf(RingDecodedEvent.Unknown(data.firstOrNull() ?: 0, data))
        val now = System.currentTimeMillis()
        return when (data[0]) {
            JringCommandId.TIME_SYNC -> listOf(
                if (data.size >= 5) {
                    RingDecodedEvent.TimeSyncAck(timestamp = getU32Le(data, 1) * 1000L)
                } else RingDecodedEvent.CommandAck(data[0])
            )
            JringCommandId.ACTIVITY_QUERY_ACK -> listOf(
                RingDecodedEvent.CommandAck(data[0])
            )
            JringCommandId.CURRENT_ACTIVITY -> listOf(
                if (data.size >= 17) {
                    val timestamp = getU32Le(data, 1) * 1000L
                    val steps = getU32Le(data, 5).toInt()
                    val distance = getU32Le(data, 9).toDouble()
                    val calories = getU32Le(data, 13).toDouble()
                    RingDecodedEvent.ActivityUpdate(timestamp, steps, distance, calories)
                } else RingDecodedEvent.Unknown(data[0], data)
            )
            JringCommandId.PERCENT_STATUS -> listOf(
                if (data.size >= 2) RingDecodedEvent.Battery(data.getUInt8(1))
                else RingDecodedEvent.Unknown(data[0], data)
            )
            JringCommandId.STATUS -> {
                val address = if (data.size >= 9) {
                    data.slice(3..8).joinToString(":") { "%02x".format(it) }
                } else null
                // Firmware string, exactly as the official app builds it in onGetDeviceInfo:
                //   CID(bytes[9-10]) + DID(bytes[11-12]) + "V" + version(bytes[1-2], LE u16)
                // e.g. 0c 8a 00 .. 3a 00 2a 00 → "003A" + "002A" + "V" + 138 = "003A002AV138".
                val firmware = if (data.size >= 13) {
                    val version = ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    val cid = ((data[10].toInt() and 0xFF) shl 8) or (data[9].toInt() and 0xFF)
                    val did = ((data[12].toInt() and 0xFF) shl 8) or (data[11].toInt() and 0xFF)
                    "%04X%04XV%d".format(cid, did, version)
                } else null
                listOf(RingDecodedEvent.Status(address, firmware))
            }
            JringCommandId.SLEEP_TIMELINE -> listOf(
                if (data.size >= 20) {
                    val timestamp = getU32Le(data, 1) * 1000L
                    val stages = (5 until 20).map { toSleepStage(data[it]) }
                    RingDecodedEvent.SleepTimeline(timestamp, stages)
                } else RingDecodedEvent.Unknown(data[0], data)
            )
            JringCommandId.HEART_RATE -> listOf(
                if (data.size >= 6) RingDecodedEvent.HeartRateSample(data.getUInt8(5), now)
                else RingDecodedEvent.Unknown(data[0], data)
            )
            JringCommandId.HISTORY_MEASUREMENT -> listOf(
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
            )
            JringCommandId.COMBINED_RESULT -> decodeCombined(data, now)
            JringCommandId.HEART_RATE_COMPLETE -> listOf(RingDecodedEvent.HeartRateComplete(now))
            JringCommandId.SPO2_COMPLETE -> listOf(RingDecodedEvent.Spo2Complete(now))
            else -> listOf(RingDecodedEvent.Unknown(data[0], data))
        }
    }

    /**
     * 0x24 combined spot-measurement response. One packet carries several metrics:
     *   [1]=HR  [2]=systolic  [3]=diastolic  [4]=SpO2%  [5]=fatigue  [6]=stress  [7]=bloodSugar(mmol/L×10)
     * Blood pressure (bytes 2-3) is a direct PPG sensor reading; blood sugar (byte 7) is a
     * profile-derived estimate the ring reports as mmol/L × 10 → we store mg/dL = (raw/10) × 18.016.
     * SpO2 (byte 4) preserves the previous "SpO2 measurement" behaviour. Only values > 0 are emitted.
     * (HRV at byte 8 is not emitted — the Jring HRV value is not reliable; HRV comes from Colmi.)
     */
    private fun decodeCombined(data: ByteArray, now: Long): List<RingDecodedEvent> {
        if (data.size < 6) return listOf(RingDecodedEvent.Unknown(data[0], data))
        val events = mutableListOf<RingDecodedEvent>()

        val hr = data.getUInt8(1)
        val systolic = data.getUInt8(2)
        val diastolic = data.getUInt8(3)
        val spo2 = data.getUInt8(4)
        val fatigue = data.getUInt8(5)
        val stress = if (data.size > 6) data.getUInt8(6) else 0
        val bloodSugarRaw = if (data.size > 7) data.getUInt8(7) else 0

        if (hr > 0) events.add(RingDecodedEvent.HeartRateSample(hr, now))
        if (systolic > 0 || diastolic > 0) events.add(
            RingDecodedEvent.HistoryMeasurement(MeasurementKind.BLOOD_PRESSURE_SYSTOLIC, systolic.toDouble(), now)
        )
        if (diastolic > 0) events.add(
            RingDecodedEvent.HistoryMeasurement(MeasurementKind.BLOOD_PRESSURE_DIASTOLIC, diastolic.toDouble(), now)
        )
        if (spo2 in 80..100) events.add(RingDecodedEvent.Spo2Result(spo2, now))
        else events.add(RingDecodedEvent.Spo2Progress(null, now))
        if (fatigue > 0) events.add(
            RingDecodedEvent.HistoryMeasurement(MeasurementKind.FATIGUE, fatigue.toDouble(), now)
        )
        if (stress > 0) events.add(RingDecodedEvent.StressSample(stress, now))
        // Blood sugar: byte[7] = mmol/L × 10 → mg/dL = (raw / 10) × 18.016
        if (bloodSugarRaw > 0) events.add(
            RingDecodedEvent.HistoryMeasurement(MeasurementKind.BLOOD_SUGAR, (bloodSugarRaw / 10.0) * 18.016, now)
        )
        return events
    }

    private fun getU32Le(data: ByteArray, offset: Int): Long = data.getUInt32Le(offset)

    private fun toSleepStage(byte: Byte): SleepStage = when (byte.toInt() and 0xFF) {
        0x28 -> SleepStage.LIGHT
        0x63 -> SleepStage.DEEP
        0x00 -> SleepStage.AWAKE
        else -> SleepStage.UNKNOWN
    }
}
