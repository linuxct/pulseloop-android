package space.linuxct.pulseloop.domain.model

enum class SleepRangeKey(val rawValue: String) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year");

    companion object {
        fun fromRaw(raw: String) = entries.firstOrNull { it.rawValue == raw } ?: WEEK
    }
}
