package space.linuxct.pulseloop.domain.model

enum class DecodeConfidence(val rawValue: String) {
    KNOWN("known"),
    PARTIAL("partial"),
    UNKNOWN("unknown");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: UNKNOWN
    }
}
