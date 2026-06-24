package space.linuxct.pulseloop.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UpdateChecker {

    sealed class Result {
        data class UpdateAvailable(val version: String) : Result()
        data object NoUpdate : Result()
        data object Error : Result()
    }

    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        try {
            val currentVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName
                ?: return@withContext Result.NoUpdate

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
                if (!response.isSuccessful) return@withContext Result.NoUpdate
                response.body?.string()
            } ?: return@withContext Result.NoUpdate

            val tagName = JSONObject(body).optString("tag_name").trimStart('v')
            if (tagName.isNotBlank() && isNewerVersion(remote = tagName, local = currentVersion)) {
                Result.UpdateAvailable(tagName)
            } else {
                Result.NoUpdate
            }
        } catch (_: Exception) {
            Result.Error
        }
    }

    fun isNewerVersion(remote: String, local: String): Boolean {
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
}
