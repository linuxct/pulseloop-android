package space.linuxct.pulseloop.ble

import space.linuxct.pulseloop.domain.model.MeasurementKind
import space.linuxct.pulseloop.domain.model.PacketDirection
import space.linuxct.pulseloop.domain.model.RingConnectionState
import space.linuxct.pulseloop.domain.model.RingDeviceType
import space.linuxct.pulseloop.domain.model.SleepStage
import space.linuxct.pulseloop.domain.model.WearableCapability

sealed class PulseEvent {
    data class DeviceStateChanged(val state: RingConnectionState, val address: String?) : PulseEvent()
    data class DeviceIdentified(val deviceType: RingDeviceType, val capabilities: Set<WearableCapability>) : PulseEvent()
    data class BatteryLevel(val percent: Int) : PulseEvent()
    data class FirmwareVersion(val version: String) : PulseEvent()
    data class RawPacket(val direction: PacketDirection, val data: ByteArray, val decoded: RingDecodedEvent) : PulseEvent()
    data class DerivedUpdate(val kind: String, val entityType: String, val entityId: String, val payloadJson: String?) : PulseEvent()
    data class ActivityUpdate(val timestamp: Long, val steps: Int, val distanceMeters: Double, val calories: Double) : PulseEvent()
    data class ActivityBucket(val timestamp: Long, val steps: Int, val distanceMeters: Double) : PulseEvent()
    data class ActivitySyncReset(val sinceDaysAgo: Int) : PulseEvent()
    data class HeartRateSample(val bpm: Int, val timestamp: Long) : PulseEvent()
    data class HeartRateComplete(val timestamp: Long) : PulseEvent()
    data class Spo2Progress(val percent: Int?, val timestamp: Long) : PulseEvent()
    data class Spo2Result(val value: Int, val timestamp: Long) : PulseEvent()
    data class Spo2Complete(val timestamp: Long) : PulseEvent()
    data class SleepTimeline(val timestamp: Long, val stages: List<SleepStage>) : PulseEvent()
    data class HistoryMeasurement(val kind: MeasurementKind, val value: Double, val timestamp: Long) : PulseEvent()
    data class StressSample(val value: Int, val timestamp: Long) : PulseEvent()
    data class HrvSample(val value: Int, val timestamp: Long) : PulseEvent()
    data class TemperatureSample(val celsius: Double, val timestamp: Long) : PulseEvent()
    data class SyncProgress(val stage: String) : PulseEvent()
    data class WorkoutStarted(val sessionId: String) : PulseEvent()
    data class WorkoutPaused(val sessionId: String) : PulseEvent()
    data class WorkoutResumed(val sessionId: String) : PulseEvent()
    data class WorkoutFinished(val sessionId: String) : PulseEvent()
    data class GpsPoint(
        val sessionId: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val accuracy: Float,
        val speed: Float?,
        val bearing: Float?,
        val accepted: Boolean,
        val rejectionReason: String?,
        val timestamp: Long
    ) : PulseEvent()
    data class CoachTrace(val message: String) : PulseEvent()
}
