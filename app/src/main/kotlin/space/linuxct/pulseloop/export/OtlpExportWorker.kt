package space.linuxct.pulseloop.export

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.export.otlp.OtlpExporter
import java.util.concurrent.TimeUnit

/**
 * Pushes health data to the OTLP endpoint. Modes: INCREMENTAL (steady-state, also the resumable
 * continuation of a big first run) and BACKFILL (re-export everything from earliest). Runs under a
 * time budget; if a backfill doesn't finish it advances the watermark and re-enqueues a continuation.
 */
@HiltWorker
class OtlpExportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val exporter: OtlpExporter,
    private val prefs: AppPreferencesDataStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val backfill = inputData.getString(KEY_MODE) == MODE_BACKFILL
        val deadline = System.currentTimeMillis() + BUDGET_MS

        return when (exporter.export(forceBackfill = backfill, deadlineMs = deadline)) {
            is OtlpExporter.Outcome.Done -> Result.success()
            is OtlpExporter.Outcome.MoreWork -> {
                enqueueContinuation(applicationContext, prefs.otlpWifiOnly.first())
                Result.success()
            }
            is OtlpExporter.Outcome.Retry -> Result.retry()
            is OtlpExporter.Outcome.Failed -> Result.failure()
            OtlpExporter.Outcome.Disabled,
            OtlpExporter.Outcome.NotConfigured -> Result.success()
        }
    }

    companion object {
        const val KEY_MODE = "mode"
        const val MODE_INCREMENTAL = "incremental"
        const val MODE_BACKFILL = "backfill"

        private const val BUDGET_MS = 8 * 60 * 1000L
        private const val PERIODIC_NAME = "otlp_export_periodic"
        private const val ONESHOT_NAME = "otlp_export_oneshot"
        private const val INCREMENTAL_NAME = "otlp_incremental"
        private const val CONTINUATION_NAME = "otlp_export_continuation"

        private fun constraints(wifiOnly: Boolean): Constraints =
            Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

        /** 12h safety-net run (catches missed sync-triggered exports). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<OtlpExportWorker>(12, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_MODE to MODE_INCREMENTAL))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Debounced sync-triggered incremental run; KEEP + delay coalesces sync bursts into one. */
        fun enqueueIncremental(context: Context, wifiOnly: Boolean) {
            val request = OneTimeWorkRequestBuilder<OtlpExportWorker>()
                .setInputData(workDataOf(KEY_MODE to MODE_INCREMENTAL))
                .setInitialDelay(60, TimeUnit.SECONDS)
                .setConstraints(constraints(wifiOnly))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                INCREMENTAL_NAME, ExistingWorkPolicy.KEEP, request
            )
        }

        /** Manual "Export now" / "Run backfill" from the settings screen. */
        fun runNow(context: Context, backfill: Boolean, wifiOnly: Boolean) {
            val request = OneTimeWorkRequestBuilder<OtlpExportWorker>()
                .setInputData(workDataOf(KEY_MODE to if (backfill) MODE_BACKFILL else MODE_INCREMENTAL))
                .setConstraints(constraints(wifiOnly))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request
            )
        }

        private fun enqueueContinuation(context: Context, wifiOnly: Boolean) {
            val request = OneTimeWorkRequestBuilder<OtlpExportWorker>()
                .setInputData(workDataOf(KEY_MODE to MODE_INCREMENTAL))
                .setConstraints(constraints(wifiOnly))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                CONTINUATION_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}
