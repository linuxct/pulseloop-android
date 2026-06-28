package space.linuxct.pulseloop.ui.charts

import androidx.compose.ui.graphics.Color
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.ui.theme.PulseColors

/**
 * Coarse clinical zones for the two metrics where a reference range is meaningful enough to colour
 * the displayed value: blood pressure and blood sugar. These are non-medical, display-only hints
 * (the rings are not medical devices). Maps to the existing semantic palette so the tint looks
 * correct in both V1 (LEGACY) and V2 (Material You, where these map onto the M3 scheme).
 */
enum class MetricZone { GOOD, ELEVATED, HIGH, LOW }

fun MetricZone.color(colors: PulseColors): Color = when (this) {
    MetricZone.GOOD     -> colors.success
    MetricZone.ELEVATED -> colors.warning
    MetricZone.HIGH     -> colors.danger
    MetricZone.LOW      -> colors.info
}

/** Short human label for the zone — "In range" / "Elevated" / "High" / "Low". */
fun MetricZone.labelRes(): Int = when (this) {
    MetricZone.GOOD     -> R.string.zone_in_range
    MetricZone.ELEVATED -> R.string.zone_elevated
    MetricZone.HIGH     -> R.string.zone_high
    MetricZone.LOW      -> R.string.zone_low
}

/** ACC/AHA-style banding by the worse of systolic/diastolic. Null when neither value is present. */
fun bloodPressureZone(systolic: Double?, diastolic: Double?): MetricZone? {
    if (systolic == null && diastolic == null) return null
    val s = systolic ?: 0.0
    val d = diastolic ?: 0.0
    return when {
        s >= 140 || d >= 90 -> MetricZone.HIGH      // hypertension stage 2
        s >= 130 || d >= 80 -> MetricZone.ELEVATED  // stage 1
        s >= 120            -> MetricZone.ELEVATED  // elevated
        s in 1.0..89.0 || d in 1.0..59.0 -> MetricZone.LOW
        else                -> MetricZone.GOOD
    }
}

/** Fasting-glucose-style banding (mg/dL). Null when no reading. */
fun glucoseZone(mgdl: Double?): MetricZone? {
    if (mgdl == null) return null
    return when {
        mgdl >= 126 -> MetricZone.HIGH
        mgdl >= 100 -> MetricZone.ELEVATED
        mgdl < 70   -> MetricZone.LOW
        else        -> MetricZone.GOOD
    }
}

/** Maps a 0–100 stress/fatigue score (lower is better) to a qualitative band string resource. */
fun lowerIsBetterBand(value: Int): Int = when {
    value < 20 -> R.string.band_excellent
    value < 40 -> R.string.band_good
    value < 60 -> R.string.band_normal
    value < 80 -> R.string.band_poor
    else       -> R.string.band_very_poor
}
