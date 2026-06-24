package space.linuxct.pulseloop.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
    val PAIRED_DEVICE_MAC     = stringPreferencesKey("paired_device_mac")
    val PAIRED_DEVICE_TYPE    = stringPreferencesKey("paired_device_type")
    val CDM_ASSOCIATION_ID    = stringPreferencesKey("cdm_association_id")
    val COACH_ENABLED         = booleanPreferencesKey("coach_enabled")
    val COACH_MODEL           = stringPreferencesKey("coach_model")
    val COACH_MORNING_HOUR    = stringPreferencesKey("coach_morning_hour")
    val COACH_EVENING_HOUR    = stringPreferencesKey("coach_evening_hour")
    val PREFERRED_UNIT_SYSTEM   = stringPreferencesKey("unit_system")
    val OPENAI_API_KEY          = stringPreferencesKey("openai_api_key")
    val OPENAI_REFRESH_TOKEN    = stringPreferencesKey("openai_refresh_token")
}
