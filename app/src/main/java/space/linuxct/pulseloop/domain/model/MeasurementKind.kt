package space.linuxct.pulseloop.domain.model

enum class MeasurementKind(val rawValue: String) {
    HEART_RATE("hr"),
    SPO2("spo2"),
    STRESS("stress"),
    HRV("hrv"),
    TEMPERATURE("temp");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: HEART_RATE
    }
}
