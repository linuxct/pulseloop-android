package space.linuxct.pulseloop.ble

interface RingSyncEngine {
    fun runStartup()
    fun handle(event: RingDecodedEvent)
    fun onDisconnected() = Unit
    fun startHeartRate() = Unit
    fun stopHeartRate() = Unit
    fun startSpO2() = Unit
    fun stopSpO2() = Unit
    /** Start a combined spot measurement (HR + BP + SpO2 + fatigue + stress + blood sugar). Jring only. */
    fun startCombinedMeasurement() = Unit
    fun stopCombinedMeasurement() = Unit
    /** Push the user's physical profile so the ring can derive blood sugar / calories. Jring only. */
    fun setUserInfo(ageYears: Int, isMale: Boolean, heightCm: Int, weightKg: Int) = Unit
    /** Push a reference cuff systolic/diastolic so the ring offsets its BP readings. Jring only. */
    fun setBloodPressureAdjust(systolic: Int, diastolic: Int) = Unit
    fun findDevice() = Unit
    fun stopFindDevice() = Unit
    fun setGoal(steps: Int) = Unit
}
