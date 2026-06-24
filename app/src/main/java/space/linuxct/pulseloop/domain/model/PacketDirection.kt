package space.linuxct.pulseloop.domain.model

enum class PacketDirection(val rawValue: String) {
    INCOMING("incoming"),
    OUTGOING("outgoing");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: INCOMING
    }
}
