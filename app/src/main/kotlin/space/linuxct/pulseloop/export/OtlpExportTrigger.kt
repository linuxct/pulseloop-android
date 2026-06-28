package space.linuxct.pulseloop.export

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.ble.EventPersistenceSubscriber
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges "the ring just synced new data" to an incremental OTLP export. Collects the post-persist
 * ticks from [EventPersistenceSubscriber], debounces them, and (when export is enabled) enqueues a
 * debounced incremental run. The WorkManager unique-work KEEP policy + initial delay coalesces the
 * per-minute foreground cadence and the connect-time history-dump burst into a single export.
 */
@Singleton
class OtlpExportTrigger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriber: EventPersistenceSubscriber,
    private val prefs: AppPreferencesDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            subscriber.dataPersisted
                .debounce(DEBOUNCE_MS)
                .collect {
                    if (prefs.otlpEnabled.first() && !prefs.otlpEndpoint.first().isNullOrBlank()) {
                        OtlpExportWorker.enqueueIncremental(context, prefs.otlpWifiOnly.first())
                    }
                }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 3_000L
    }
}
