package space.linuxct.pulseloop.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.MainActivity
import space.linuxct.pulseloop.R
import javax.inject.Inject

@AndroidEntryPoint
class LiveWorkoutService : Service() {

    @Inject lateinit var liveWorkoutManager: LiveWorkoutManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getManager(): LiveWorkoutManager = liveWorkoutManager
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Workout in progress", null, null),
            if (Build.VERSION.SDK_INT >= 29)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            else 0
        )
        serviceScope.launch {
            liveWorkoutManager.liveState.collect { state ->
                if (state != null) {
                    updateNotification(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun updateNotification(state: WorkoutLiveState) {
        val elapsed = formatElapsed(state.elapsedSeconds)
        val hrText  = state.latestHR?.let { " · $it bpm" } ?: ""
        val distText = if (state.distanceMeters >= 50) " · %.2f km".format(state.distanceMeters / 1000.0) else ""
        val content  = "$elapsed$hrText$distText"
        val title    = buildActivityTitle(state)

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, content, state.sessionId))
    }

    private fun buildActivityTitle(state: WorkoutLiveState): String {
        val base = when (state.activityType) {
            "run"   -> "Running"
            "walk"  -> "Walking"
            "cycle" -> "Cycling"
            "swim"  -> "Swimming"
            "hike"  -> "Hiking"
            else    -> "Workout"
        }
        return if (state.status == "paused") "$base (paused)" else base
    }

    private fun buildNotification(title: String, content: String?, sessionId: String?): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            sessionId?.let { putExtra("workout_session_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_workout)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Live Workout",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active workout status"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun formatElapsed(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    companion object {
        private const val CHANNEL_ID       = "live_workout"
        private const val NOTIFICATION_ID  = 2001
    }
}
