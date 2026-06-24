package space.linuxct.pulseloop.workout

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.linuxct.pulseloop.ble.PulseEvent
import space.linuxct.pulseloop.ble.PulseEventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class GpsRouteRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _pointCount = MutableStateFlow(0)
    val pointCount: StateFlow<Int> = _pointCount.asStateFlow()

    private var sessionId: String? = null
    private var lastAccepted: Location? = null
    private var isCycling = false

    private val maxAccuracyMeters = 30f
    private val maxFixAgeMs       = 5_000L
    private val maxCourseDelta    = 25.0

    fun start(sessionId: String, activityType: String = "run") {
        this.sessionId = sessionId
        this.isCycling = activityType == "cycle"
        lastAccepted = null
        _pointCount.value = 0

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateDistanceMeters(if (isCycling) 8f else 5f)
            .setWaitForAccurateLocation(false)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        _isTracking.value = true
    }

    fun stop() {
        fusedClient.removeLocationUpdates(locationCallback)
        lastAccepted = null
        sessionId = null
        _isTracking.value = false
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val sid = sessionId ?: return
            for (loc in result.locations) {
                val reason = rejectionReason(loc)
                val accepted = reason == null
                if (accepted) {
                    lastAccepted = loc
                    _pointCount.value = _pointCount.value + 1
                }
                PulseEventBus.publish(PulseEvent.GpsPoint(
                    sessionId    = sid,
                    latitude     = loc.latitude,
                    longitude    = loc.longitude,
                    altitude     = if (loc.hasAltitude()) loc.altitude else null,
                    accuracy     = loc.accuracy,
                    speed        = if (loc.hasSpeed()) loc.speed else null,
                    bearing      = if (loc.hasBearing()) loc.bearing else null,
                    accepted     = accepted,
                    rejectionReason = reason,
                    timestamp    = loc.time
                ))
            }
        }
    }

    private fun rejectionReason(loc: Location): String? {
        if (loc.accuracy <= 0 || loc.accuracy > maxAccuracyMeters) return "accuracy"
        if (System.currentTimeMillis() - loc.time > maxFixAgeMs) return "stale"
        val last = lastAccepted ?: return null
        val distance = loc.distanceTo(last)
        val dt = (loc.time - last.time) / 1000.0
        val maxSpeed = if (isCycling) 25.0f else 8.0f
        if (dt > 0 && distance / dt > maxSpeed) return "speed"
        val minMove = if (isCycling) 8f else 4f
        if (distance >= minMove) return null
        if (last.hasBearing() && loc.hasBearing()) {
            var delta = loc.bearing.toDouble() - last.bearing.toDouble()
            while (delta > 180) delta -= 360
            while (delta < -180) delta += 360
            if (kotlin.math.abs(delta) > maxCourseDelta) return null
        }
        val minIntervalMs = if (isCycling) 8_000L else 6_000L
        if (loc.time - last.time >= minIntervalMs) return null
        return "stationary"
    }
}
