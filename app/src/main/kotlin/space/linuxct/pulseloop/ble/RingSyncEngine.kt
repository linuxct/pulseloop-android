package space.linuxct.pulseloop.ble

interface RingSyncEngine {
    fun runStartup()
    fun handle(event: RingDecodedEvent)
    fun startHeartRate() = Unit
    fun stopHeartRate() = Unit
    fun startSpO2() = Unit
    fun stopSpO2() = Unit
    fun findDevice() = Unit
    fun setGoal(steps: Int) = Unit
}
