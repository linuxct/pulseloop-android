package space.linuxct.pulseloop.domain.model

enum class SleepStage(val rawValue: String) {
    LIGHT("light"),
    DEEP("deep"),
    AWAKE("awake"),
    UNKNOWN("unknown"),
    REM("rem");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: UNKNOWN
    }
}
