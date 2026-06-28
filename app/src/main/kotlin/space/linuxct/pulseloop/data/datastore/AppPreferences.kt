package space.linuxct.pulseloop.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    val USE_MATERIAL_YOU        = booleanPreferencesKey("use_material_you")

    // OpenTelemetry (OTLP) export — non-secret config. Secrets (password/token/header value)
    // live in SecretStore (EncryptedSharedPreferences), never here.
    val OTLP_ENABLED            = booleanPreferencesKey("otlp_enabled")
    val OTLP_ENDPOINT           = stringPreferencesKey("otlp_endpoint")
    val OTLP_AUTH_TYPE          = stringPreferencesKey("otlp_auth_type")   // NONE | BASIC | BEARER
    val OTLP_USERNAME           = stringPreferencesKey("otlp_username")
    val OTLP_HEADER_NAME        = stringPreferencesKey("otlp_header_name") // e.g. X-Scope-OrgID
    val OTLP_INCLUDE_GPS        = booleanPreferencesKey("otlp_include_gps")
    val OTLP_WIFI_ONLY          = booleanPreferencesKey("otlp_wifi_only")
    val OTLP_INSTANCE_ID        = stringPreferencesKey("otlp_instance_id") // stable service.instance.id

    // Master opt-in for the ring's estimated blood-pressure / blood-sugar readings. OFF by default:
    // until the user enables it in Settings, those metrics are hidden everywhere AND not written to
    // the DB. Enabling starts capture from that moment on.
    val BLOOD_METRICS_ENABLED   = booleanPreferencesKey("blood_metrics_enabled")

    // Blood-pressure / blood-sugar calibration (Jring/56ff only). BP reference values are also
    // pushed to the ring (0x33) so it offsets its own readings; both BP and glucose are additionally
    // applied as display offsets in the ViewModels. 0 = unset / not calibrated.
    val BP_CAL_SYSTOLIC         = intPreferencesKey("bp_cal_systolic")
    val BP_CAL_DIASTOLIC        = intPreferencesKey("bp_cal_diastolic")
    val GLUCOSE_OFFSET_MGDL     = doublePreferencesKey("glucose_offset_mgdl") // applied to displayed glucose
    val GLUCOSE_REF_MGDL        = doublePreferencesKey("glucose_ref_mgdl")    // last entered reference (UI repopulation)
}
