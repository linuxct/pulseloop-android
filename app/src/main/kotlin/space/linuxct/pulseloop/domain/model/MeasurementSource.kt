package space.linuxct.pulseloop.domain.model

enum class MeasurementSource(val rawValue: String) {
    RING("ring"),
    MOCK("mock"),
    HISTORY("history"),
    WORKOUT("workout"),
    MANUAL("manual"),
    LIVE("live"),
    COLMI("colmi");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: RING
    }
}
