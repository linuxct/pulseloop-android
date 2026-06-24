package space.linuxct.pulseloop.core.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineUtil {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun totalDistanceMeters(points: List<Pair<Double, Double>>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            val (lat1, lon1) = points[i - 1]
            val (lat2, lon2) = points[i]
            total += distanceMeters(lat1, lon1, lat2, lon2)
        }
        return total
    }

    fun paceSecPerKm(distanceMeters: Double, elapsedSeconds: Long): Double? {
        if (distanceMeters < 50.0 || elapsedSeconds <= 0L) return null
        val km = distanceMeters / 1000.0
        return elapsedSeconds / km
    }
}
