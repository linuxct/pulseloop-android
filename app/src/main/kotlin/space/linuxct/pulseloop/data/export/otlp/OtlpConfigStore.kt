package space.linuxct.pulseloop.data.export.otlp

import kotlinx.coroutines.flow.first
import space.linuxct.pulseloop.BuildConfig
import space.linuxct.pulseloop.data.datastore.AppPreferencesDataStore
import space.linuxct.pulseloop.data.datastore.SecretStore
import space.linuxct.pulseloop.data.db.dao.DeviceDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles a current [OtlpExportConfig] snapshot from the plaintext DataStore (non-secret),
 * the encrypted [SecretStore] (credentials), and the paired device (for `device.model`).
 * Used by the worker and the "test connection" path; the UI binds the individual flows directly.
 */
@Singleton
class OtlpConfigStore @Inject constructor(
    private val prefs: AppPreferencesDataStore,
    private val secrets: SecretStore,
    private val deviceDao: DeviceDao,
) {
    suspend fun snapshot(): OtlpExportConfig {
        val authType = runCatching {
            OtlpAuthType.valueOf(prefs.otlpAuthType.first())
        }.getOrDefault(OtlpAuthType.NONE)

        return OtlpExportConfig(
            enabled = prefs.otlpEnabled.first(),
            endpoint = prefs.otlpEndpoint.first().orEmpty(),
            authType = authType,
            username = prefs.otlpUsername.first(),
            password = secrets.getPassword(),
            bearerToken = secrets.getBearerToken(),
            headerName = prefs.otlpHeaderName.first(),
            headerValue = secrets.getHeaderValue(),
            includeGps = prefs.otlpIncludeGps.first(),
            wifiOnly = prefs.otlpWifiOnly.first(),
            instanceId = prefs.getOrCreateOtlpInstanceId(),
            deviceModel = deviceDao.getDevice()?.deviceTypeRaw,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }
}
