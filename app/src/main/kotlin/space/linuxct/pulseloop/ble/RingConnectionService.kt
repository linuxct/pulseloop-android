package space.linuxct.pulseloop.ble

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import space.linuxct.pulseloop.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.PulseLoopApp
import space.linuxct.pulseloop.R
import javax.inject.Inject

@AndroidEntryPoint
class RingConnectionService : Service() {

    @Inject lateinit var bleClient: RingBLEClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationId = 3001

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this, notificationId, buildNotification(null),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        scope.launch {
            bleClient.batteryPercent.collect { pct ->
                getSystemService(NotificationManager::class.java)
                    ?.notify(notificationId, buildNotification(pct))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(batteryPct: Int?): Notification {
        val body = if (batteryPct != null) "Ring connected · $batteryPct%" else "Ring connected"
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, PulseLoopApp.CHANNEL_RING)
            .setSmallIcon(R.drawable.ic_workout)
            .setContentTitle("PulseLoop")
            .setContentText(body)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
