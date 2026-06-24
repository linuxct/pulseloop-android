package space.linuxct.pulseloop.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import space.linuxct.pulseloop.PulseLoopApp
import space.linuxct.pulseloop.R
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = UpdateChecker.check(context)
        if (result is UpdateChecker.Result.UpdateAvailable) {
            postUpdateNotification(result.version)
        }
        return Result.success()
    }

    private fun postUpdateNotification(newVersion: String) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, PulseLoopApp.CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_workout)
            .setContentTitle("PulseLoop $newVersion available")
            .setContentText("Tap to download the latest version")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val RELEASES_URL = "https://github.com/linuxct/pulseloop-android/releases/latest"
        private const val WORK_NAME = "update_check_daily"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
