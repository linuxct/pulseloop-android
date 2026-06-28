package space.linuxct.pulseloop.domain.model

enum class MeasurementKind(val rawValue: String) {
    HEART_RATE("hr"),
    SPO2("spo2"),
    STRESS("stress"),
    HRV("hrv"),
    TEMPERATURE("temp"),
    // Jring (56ff) combined-measurement metrics. Blood pressure is stored as two separate
    // kinds at the same timestamp (the measurements table keys on kindRaw, so sys/dia coexist).
    BLOOD_PRESSURE_SYSTOLIC("bp_sys"),
    BLOOD_PRESSURE_DIASTOLIC("bp_dia"),
    BLOOD_SUGAR("glucose"),
    FATIGUE("fatigue");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: HEART_RATE
    }
}
