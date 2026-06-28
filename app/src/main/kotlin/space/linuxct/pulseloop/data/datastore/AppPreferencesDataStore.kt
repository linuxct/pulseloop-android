package space.linuxct.pulseloop.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.ONBOARDING_COMPLETED] ?: false }

    val pairedDeviceMac: Flow<String?> =
        dataStore.data.map { it[AppPreferences.PAIRED_DEVICE_MAC] }

    val pairedDeviceType: Flow<String?> =
        dataStore.data.map { it[AppPreferences.PAIRED_DEVICE_TYPE] }

    val cdmAssociationId: Flow<String?> =
        dataStore.data.map { it[AppPreferences.CDM_ASSOCIATION_ID] }

    val coachEnabled: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.COACH_ENABLED] ?: false }

    val openAiKey: Flow<String?> =
        dataStore.data.map { it[AppPreferences.OPENAI_API_KEY] }

    val openAiRefreshToken: Flow<String?> =
        dataStore.data.map { it[AppPreferences.OPENAI_REFRESH_TOKEN] }

    val coachModel: Flow<String> =
        dataStore.data.map { it[AppPreferences.COACH_MODEL] ?: DEFAULT_COACH_MODEL }

    val useMaterialYou: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.USE_MATERIAL_YOU] ?: false }

    suspend fun initCoachModelDefault() = dataStore.edit { prefs ->
        if (!prefs.contains(AppPreferences.COACH_MODEL)) {
            prefs[AppPreferences.COACH_MODEL] = DEFAULT_COACH_MODEL
        }
    }

    suspend fun setOpenAiKey(key: String?) = dataStore.edit {
        if (key.isNullOrBlank()) it.remove(AppPreferences.OPENAI_API_KEY)
        else it[AppPreferences.OPENAI_API_KEY] = key.trim()
    }

    suspend fun setOpenAiTokens(accessToken: String, refreshToken: String) = dataStore.edit {
        it[AppPreferences.OPENAI_API_KEY]       = accessToken
        it[AppPreferences.OPENAI_REFRESH_TOKEN] = refreshToken
    }

    suspend fun clearOpenAiAuth() = dataStore.edit {
        it.remove(AppPreferences.OPENAI_API_KEY)
        it.remove(AppPreferences.OPENAI_REFRESH_TOKEN)
    }

    suspend fun setCoachModel(model: String) =
        dataStore.edit { it[AppPreferences.COACH_MODEL] = model }

    suspend fun setUseMaterialYou(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.USE_MATERIAL_YOU] = enabled }

    suspend fun setOnboardingCompleted(value: Boolean) =
        dataStore.edit { it[AppPreferences.ONBOARDING_COMPLETED] = value }

    suspend fun setPairedDevice(mac: String, type: String, associationId: Int) {
        dataStore.edit {
            it[AppPreferences.PAIRED_DEVICE_MAC] = mac
            it[AppPreferences.PAIRED_DEVICE_TYPE] = type
            it[AppPreferences.CDM_ASSOCIATION_ID] = associationId.toString()
        }
    }

    suspend fun clearPairedDevice() {
        dataStore.edit {
            it.remove(AppPreferences.PAIRED_DEVICE_MAC)
            it.remove(AppPreferences.PAIRED_DEVICE_TYPE)
            it.remove(AppPreferences.CDM_ASSOCIATION_ID)
        }
    }

    suspend fun setCoachEnabled(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.COACH_ENABLED] = enabled }

    // ── OpenTelemetry (OTLP) export config ──────────────────────────────────

    val otlpEnabled: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.OTLP_ENABLED] ?: false }

    val otlpEndpoint: Flow<String?> =
        dataStore.data.map { it[AppPreferences.OTLP_ENDPOINT] }

    val otlpAuthType: Flow<String> =
        dataStore.data.map { it[AppPreferences.OTLP_AUTH_TYPE] ?: "NONE" }

    val otlpUsername: Flow<String?> =
        dataStore.data.map { it[AppPreferences.OTLP_USERNAME] }

    val otlpHeaderName: Flow<String?> =
        dataStore.data.map { it[AppPreferences.OTLP_HEADER_NAME] }

    val otlpIncludeGps: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.OTLP_INCLUDE_GPS] ?: false }

    val otlpWifiOnly: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.OTLP_WIFI_ONLY] ?: false }

    suspend fun setOtlpEnabled(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.OTLP_ENABLED] = enabled }

    suspend fun setOtlpEndpoint(url: String?) = dataStore.edit {
        if (url.isNullOrBlank()) it.remove(AppPreferences.OTLP_ENDPOINT)
        else it[AppPreferences.OTLP_ENDPOINT] = url.trim()
    }

    suspend fun setOtlpAuthType(type: String) =
        dataStore.edit { it[AppPreferences.OTLP_AUTH_TYPE] = type }

    suspend fun setOtlpUsername(name: String?) = dataStore.edit {
        if (name.isNullOrBlank()) it.remove(AppPreferences.OTLP_USERNAME)
        else it[AppPreferences.OTLP_USERNAME] = name.trim()
    }

    suspend fun setOtlpHeaderName(name: String?) = dataStore.edit {
        if (name.isNullOrBlank()) it.remove(AppPreferences.OTLP_HEADER_NAME)
        else it[AppPreferences.OTLP_HEADER_NAME] = name.trim()
    }

    suspend fun setOtlpIncludeGps(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.OTLP_INCLUDE_GPS] = enabled }

    suspend fun setOtlpWifiOnly(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.OTLP_WIFI_ONLY] = enabled }

    // ── Blood-pressure / blood-sugar opt-in + calibration (Jring only) ──────

    /** Master switch for the ring's estimated BP/blood-sugar. OFF by default. */
    val bloodMetricsEnabled: Flow<Boolean> =
        dataStore.data.map { it[AppPreferences.BLOOD_METRICS_ENABLED] ?: false }

    suspend fun setBloodMetricsEnabled(enabled: Boolean) =
        dataStore.edit { it[AppPreferences.BLOOD_METRICS_ENABLED] = enabled }


    /** Reference systolic from a cuff. 0 = not set. Also pushed to the ring (0x33). */
    val bpCalSystolic: Flow<Int> =
        dataStore.data.map { it[AppPreferences.BP_CAL_SYSTOLIC] ?: 0 }

    /** Reference diastolic from a cuff. 0 = not set. Also pushed to the ring (0x33). */
    val bpCalDiastolic: Flow<Int> =
        dataStore.data.map { it[AppPreferences.BP_CAL_DIASTOLIC] ?: 0 }

    /** Display offset (mg/dL) applied to the ring's profile-derived glucose. 0 = not calibrated. */
    val glucoseOffsetMgdl: Flow<Double> =
        dataStore.data.map { it[AppPreferences.GLUCOSE_OFFSET_MGDL] ?: 0.0 }

    /** Last reference glucose (mg/dL) the user entered, persisted only to repopulate the field. */
    val glucoseRefMgdl: Flow<Double> =
        dataStore.data.map { it[AppPreferences.GLUCOSE_REF_MGDL] ?: 0.0 }

    suspend fun setBpCalibration(systolic: Int, diastolic: Int) = dataStore.edit {
        it[AppPreferences.BP_CAL_SYSTOLIC] = systolic.coerceIn(0, 300)
        it[AppPreferences.BP_CAL_DIASTOLIC] = diastolic.coerceIn(0, 300)
    }

    suspend fun setGlucoseCalibration(offsetMgdl: Double, refMgdl: Double) = dataStore.edit {
        it[AppPreferences.GLUCOSE_OFFSET_MGDL] = offsetMgdl
        it[AppPreferences.GLUCOSE_REF_MGDL] = refMgdl
    }

    suspend fun clearGlucoseCalibration() = dataStore.edit {
        it.remove(AppPreferences.GLUCOSE_OFFSET_MGDL)
        it.remove(AppPreferences.GLUCOSE_REF_MGDL)
    }

    /** Returns a stable per-install id for `service.instance.id`, generating it on first use. */
    suspend fun getOrCreateOtlpInstanceId(): String {
        dataStore.data.first()[AppPreferences.OTLP_INSTANCE_ID]?.let { return it }
        val id = UUID.randomUUID().toString()
        dataStore.edit { it[AppPreferences.OTLP_INSTANCE_ID] = id }
        return id
    }

    companion object {
        const val DEFAULT_COACH_MODEL = "gpt-5.4"
    }
}
