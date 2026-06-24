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
        writer.enqueue(encoder.makeAutomaticHeartRateCommand(enabled = true, cadenceMinutes = 15))
    }

    override fun startSpO2() {
        writer.enqueue(encoder.makeSpO2StartCommand())
    }

    override fun stopSpO2() {
        writer.enqueue(encoder.makeSpO2StopCommand())
    }

    override fun findDevice() {
        writer.enqueue(encoder.makeFindRingCommand())
    }

    override fun setGoal(steps: Int) {
        writer.enqueue(encoder.makeGoalCommand(steps))
    }
}
