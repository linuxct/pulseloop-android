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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import space.linuxct.pulseloop.PulseLoopApp
import space.linuxct.pulseloop.R
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
                ?: return@withContext Result.success()

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.github.com/repos/linuxct/pulseloop-android/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "PulseLoop-Android/$currentVersion")
                .build()

            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.success()
                response.body?.string()
            } ?: return@withContext Result.success()

            val tagName = JSONObject(body).optString("tag_name").trimStart('v')
            if (tagName.isNotBlank() && isNewerVersion(remote = tagName, local = currentVersion)) {
                postUpdateNotification(tagName)
            }

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split('.').mapNotNull { it.toIntOrNull() }
        val l = local.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
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
