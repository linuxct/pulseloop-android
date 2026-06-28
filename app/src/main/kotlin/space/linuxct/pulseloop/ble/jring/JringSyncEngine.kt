package space.linuxct.pulseloop.ble.jring

import space.linuxct.pulseloop.ble.RingCommandWriter
import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.ble.RingSyncEngine

class JringSyncEngine(private val writer: RingCommandWriter) : RingSyncEngine {

    private val encoder = RingEncoder()

    override fun runStartup() {
        writer.enqueue(encoder.makeStatusCommand())
        writer.enqueue(encoder.makeTimeSyncCommand())
        writer.enqueue(encoder.makeLocaleCommand())
        writer.enqueue(encoder.makeActivityQueryCommand())
        writer.enqueue(encoder.makeHistoryQueryCommand())
        writer.enqueue(encoder.makeHistoryMeasurementQueryCommand())
    }

    override fun handle(event: RingDecodedEvent) {
        // jring is fire-and-forget; no response-driven state machine to advance.
    }

    override fun startHeartRate() {
        writer.enqueue(encoder.makeHeartRateStartCommand())
    }

    override fun stopHeartRate() {
        writer.enqueue(encoder.makeHeartRateStopCommand())
        // Restore the ring's autonomous background HR cadence (0x19) so the ring keeps
        // measuring on its own schedule when the app is offline. Window 00:00-23:59, 15 min cadence.
        writer.enqueue(encoder.makeAutomaticHeartRateCommand(enabled = true, cadenceMinutes = 15))
    }

    override fun startSpO2() {
        writer.enqueue(encoder.makeSpO2StartCommand())
    }

    override fun stopSpO2() {
        writer.enqueue(encoder.makeSpO2StopCommand())
    }

    override fun startCombinedMeasurement() {
        writer.enqueue(encoder.makeCombinedMeasurementStartCommand())
    }

    override fun stopCombinedMeasurement() {
        writer.enqueue(encoder.makeCombinedMeasurementStopCommand())
    }

    override fun setUserInfo(ageYears: Int, isMale: Boolean, heightCm: Int, weightKg: Int) {
        writer.enqueue(encoder.makeUserInfoCommand(ageYears, isMale, heightCm, weightKg))
    }

    override fun setBloodPressureAdjust(systolic: Int, diastolic: Int) {
        writer.enqueue(encoder.makeBPAdjustCommand(systolic, diastolic))
    }

    override fun findDevice() {
        writer.enqueue(encoder.makeFindRingCommand())
    }

    override fun stopFindDevice() {
        writer.enqueue(encoder.makeStopFindRingCommand())
    }

    override fun setGoal(steps: Int) {
        writer.enqueue(encoder.makeGoalCommand(steps))
    }
}
