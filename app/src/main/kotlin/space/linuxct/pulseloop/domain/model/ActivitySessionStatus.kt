package space.linuxct.pulseloop.domain.model

enum class ActivitySessionStatus(val rawValue: String) {
    RECORDING("recording"),
    PAUSED("paused"),
    FINISHED("finished"),
    CANCELLED("cancelled");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: RECORDING
    }
}
