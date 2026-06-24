package space.linuxct.pulseloop.domain.model

enum class RingConnectionState(val rawValue: String) {
    IDLE("idle"),
    SCANNING("scanning"),
    CONNECTING("connecting"),
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    RECONNECTING("reconnecting"),
    FAILED("failed");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: IDLE
    }
}
