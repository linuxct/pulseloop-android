package space.linuxct.pulseloop.ble

import space.linuxct.pulseloop.domain.model.DecodeConfidence
import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.SleepStage

sealed class RingDecodedEvent {
    data class ActivityUpdate(val timestamp: Long, val steps: Int, val distanceMeters: Double, val calories: Double) : RingDecodedEvent()
    data class ActivityBucket(val timestamp: Long, val steps: Int, val distanceMeters: Double) : RingDecodedEvent()
    data class HeartRateSample(val bpm: Int, val timestamp: Long) : RingDecodedEvent()
    data class HeartRateComplete(val timestamp: Long) : RingDecodedEvent()
    data class Spo2Progress(val percent: Int?, val timestamp: Long) : RingDecodedEvent()
    data class Spo2Result(val value: Int, val timestamp: Long) : RingDecodedEvent()
    data class Spo2Complete(val timestamp: Long) : RingDecodedEvent()
    data class SleepTimeline(val timestamp: Long, val stages: List<SleepStage>) : RingDecodedEvent()
    data class HistoryMeasurement(val kind: MeasurementKind, val value: Double, val timestamp: Long) : RingDecodedEvent()
    data class StressSample(val value: Int, val timestamp: Long) : RingDecodedEvent()
    data class HrvSample(val value: Int, val timestamp: Long) : RingDecodedEvent()
    data class TemperatureSample(val celsius: Double, val timestamp: Long) : RingDecodedEvent()
    data class HistorySyncProgress(val stage: String) : RingDecodedEvent()
    object HistorySyncFinished : RingDecodedEvent()
    data class Battery(val percent: Int) : RingDecodedEvent()
    data class Status(val address: String?, val firmware: String? = null) : RingDecodedEvent()
    data class TimeSyncAck(val timestamp: Long) : RingDecodedEvent()
    data class CommandAck(val commandId: Byte) : RingDecodedEvent()
    data class Unknown(val commandId: Byte, val raw: ByteArray) : RingDecodedEvent()

    val eventKind: String get() = when (this) {
        is ActivityUpdate     -> "activity"
        is ActivityBucket     -> "activity_bucket"
        is HeartRateSample    -> "hr_sample"
        is HeartRateComplete  -> "hr_complete"
        is Spo2Progress       -> "spo2_progress"
        is Spo2Result         -> "spo2_result"
        is Spo2Complete       -> "spo2_complete"
        is SleepTimeline      -> "sleep_timeline"
        is HistoryMeasurement -> "history_measurement"
        is StressSample       -> "stress_sample"
        is HrvSample          -> "hrv_sample"
        is TemperatureSample  -> "temperature_sample"
        is HistorySyncProgress -> "history_sync_progress"
        is HistorySyncFinished -> "history_sync_finished"
        is Battery            -> "battery"
        is Status             -> "status"
        is TimeSyncAck        -> "time_sync_ack"
        is CommandAck         -> "command_ack"
        is Unknown            -> "unknown"
    }

    val confidence: DecodeConfidence get() = when (this) {
        is Unknown            -> DecodeConfidence.UNKNOWN
        is CommandAck, is HeartRateComplete, is Spo2Complete, is Spo2Progress -> DecodeConfidence.PARTIAL
        else                  -> DecodeConfidence.KNOWN
    }
}
