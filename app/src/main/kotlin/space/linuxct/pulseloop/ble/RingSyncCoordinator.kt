package space.linuxct.pulseloop.ble

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingSyncCoordinator @Inject constructor(
    val ringBLEClient: RingBLEClient
) {
    fun measureHR()  = ringBLEClient.measureHR()
    fun stopHR()     = ringBLEClient.stopHR()
    fun measureSpO2() = ringBLEClient.measureSpO2()
    fun stopSpO2()   = ringBLEClient.stopSpO2()
    fun findDevice() = ringBLEClient.findDevice()
    fun setDailyStepGoal(steps: Int) = ringBLEClient.setGoal(steps)
}
