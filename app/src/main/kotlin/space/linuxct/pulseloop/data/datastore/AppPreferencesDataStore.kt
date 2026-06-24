package space.linuxct.pulseloop.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    val coachModel: Flow<String?> =
        dataStore.data.map { it[AppPreferences.COACH_MODEL] }

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
}
