package space.linuxct.pulseloop.domain.model

enum class RingDeviceType(val rawValue: String) {
    JRING("jring"),
    COLMI("colmi");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: JRING
    }
}
