package space.linuxct.pulseloop.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.linuxct.pulseloop.R
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.datastore.SecretStore
import space.linuxct.pulseloop.data.db.dao.OtlpExportStateDao
import space.linuxct.pulseloop.data.db.entities.OtlpExportStateEntity
import space.linuxct.pulseloop.data.export.otlp.OtlpConfigStore
import space.linuxct.pulseloop.data.export.otlp.OtlpExporter
import space.linuxct.pulseloop.data.export.otlp.SendResult
import space.linuxct.pulseloop.export.OtlpExportWorker
import javax.inject.Inject

sealed interface ExportState {
    data object Idle : ExportState
    data object Testing : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}

@HiltViewModel
class DataExportViewModel @Inject constructor(
    private val prefs: AppPreferencesDataStore,
    private val secrets: SecretStore,
    private val configStore: OtlpConfigStore,
    private val exporter: OtlpExporter,
    private val stateDao: OtlpExportStateDao,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private fun <T> flow(source: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        source.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    // Toggles / chips have no text cursor, so a derived (async) flow is fine for them.
    val enabled = flow(prefs.otlpEnabled, false)
    val authType = flow(prefs.otlpAuthType, "NONE")
    val includeGps = flow(prefs.otlpIncludeGps, false)
    val wifiOnly = flow(prefs.otlpWifiOnly, false)

    // Editable TEXT fields are backed by synchronous MutableStateFlows so the displayed value
    // changes in the SAME frame as the keystroke. Binding a TextField straight to an async
    // DataStore/SecretStore round-trip makes the value lag a frame, which resets the cursor to
    // position 0 on every keystroke. We seed these once (init) and persist in the background.
    private val _endpoint = MutableStateFlow("")
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()
    private val _headerName = MutableStateFlow("")
    val headerName: StateFlow<String> = _headerName.asStateFlow()
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    private val _bearerToken = MutableStateFlow("")
    val bearerToken: StateFlow<String> = _bearerToken.asStateFlow()
    private val _headerValue = MutableStateFlow("")
    val headerValue: StateFlow<String> = _headerValue.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _statuses = MutableStateFlow<List<OtlpExportStateEntity>>(emptyList())
    val statuses: StateFlow<List<OtlpExportStateEntity>> = _statuses.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed the editable text fields once from persisted storage.
            _endpoint.value = prefs.otlpEndpoint.first() ?: ""
            _username.value = prefs.otlpUsername.first() ?: ""
            _headerName.value = prefs.otlpHeaderName.first() ?: ""
            _password.value = secrets.getPassword() ?: ""
            _bearerToken.value = secrets.getBearerToken() ?: ""
            _headerValue.value = secrets.getHeaderValue() ?: ""
            refreshStatuses()
        }
    }

    fun setEnabled(v: Boolean) = viewModelScope.launch { prefs.setOtlpEnabled(v) }
    fun setAuthType(v: String) = viewModelScope.launch { prefs.setOtlpAuthType(v) }
    fun setIncludeGps(v: Boolean) = viewModelScope.launch { prefs.setOtlpIncludeGps(v) }
    fun setWifiOnly(v: Boolean) = viewModelScope.launch { prefs.setOtlpWifiOnly(v) }

    // Text setters: update the in-memory value SYNCHRONOUSLY (keeps the cursor), persist in background.
    fun setEndpoint(v: String) { _endpoint.value = v; viewModelScope.launch { prefs.setOtlpEndpoint(v) } }
    fun setUsername(v: String) { _username.value = v; viewModelScope.launch { prefs.setOtlpUsername(v) } }
    fun setHeaderName(v: String) { _headerName.value = v; viewModelScope.launch { prefs.setOtlpHeaderName(v) } }
    fun setPassword(v: String) { _password.value = v; viewModelScope.launch { secrets.setPassword(v) } }
    fun setBearerToken(v: String) { _bearerToken.value = v; viewModelScope.launch { secrets.setBearerToken(v) } }
    fun setHeaderValue(v: String) { _headerValue.value = v; viewModelScope.launch { secrets.setHeaderValue(v) } }

    fun testConnection() = viewModelScope.launch {
        _exportState.value = ExportState.Testing
        val cfg = configStore.snapshot()
        if (!cfg.isConfigured) {
            _exportState.value = ExportState.Error(appContext.getString(R.string.otlp_msg_enter_endpoint))
            return@launch
        }
        _exportState.value = when (val r = exporter.testConnection(cfg)) {
            is SendResult.Success -> ExportState.Success(appContext.getString(R.string.otlp_msg_test_ok))
            is SendResult.Retryable -> ExportState.Error(appContext.getString(R.string.otlp_msg_busy))
            SendResult.PayloadTooLarge -> ExportState.Error(appContext.getString(R.string.otlp_msg_payload_too_large))
            is SendResult.Fatal -> ExportState.Error(appContext.getString(R.string.otlp_msg_failed, r.code, r.message))
        }
    }

    fun exportNow() = viewModelScope.launch {
        OtlpExportWorker.runNow(appContext, backfill = false, wifiOnly = wifiOnly.value)
        _exportState.value = ExportState.Success(appContext.getString(R.string.otlp_msg_incremental_scheduled))
    }

    fun runBackfill() = viewModelScope.launch {
        OtlpExportWorker.runNow(appContext, backfill = true, wifiOnly = wifiOnly.value)
        _exportState.value = ExportState.Success(appContext.getString(R.string.otlp_msg_backfill_scheduled))
    }

    fun refreshStatuses() = viewModelScope.launch { _statuses.value = stateDao.getAll() }

    fun clearStatus() { _exportState.value = ExportState.Idle }
}
