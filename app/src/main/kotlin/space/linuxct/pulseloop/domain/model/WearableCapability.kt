package space.linuxct.pulseloop.domain.model

enum class WearableCapability(val rawValue: String) {
    HEART_RATE("heartRate"),
    SPO2("spo2"),
    STEPS("steps"),
    SLEEP("sleep"),
    BATTERY("battery"),
    REM_SLEEP("remSleep"),
    STRESS("stress"),
    HRV("hrv"),
    TEMPERATURE("temperature"),
    BLOOD_PRESSURE("bloodPressure"),
    BLOOD_SUGAR("bloodSugar"),
    FATIGUE("fatigue"),
    MANUAL_HEART_RATE("manualHeartRate"),
    MANUAL_SPO2("manualSpo2"),
    REALTIME_HEART_RATE("realtimeHeartRate"),
    REALTIME_STEPS("realtimeSteps"),
    FIND_DEVICE("findDevice"),
    POWER_OFF("powerOff"),
    FACTORY_RESET("factoryReset");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw }
        fun fromCsv(csv: String): Set<WearableCapability> =
            csv.split(",").mapNotNull { fromRaw(it.trim()) }.toSet()
        fun Set<WearableCapability>.toCsv(): String = joinToString(",") { it.rawValue }
    }
}
