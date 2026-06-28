package space.linuxct.pulseloop.data.export.otlp

enum class OtlpAuthType { NONE, BASIC, BEARER }

/**
 * Logical data types the exporter handles. [key] doubles as the `otlp_export_state` cursor key;
 * for the five measurement types it is also the `measurements.kindRaw` value.
 */
enum class OtlpDataType(val key: String) {
    HEART_RATE("hr"),
    SPO2("spo2"),
    HRV("hrv"),
    STRESS("stress"),
    TEMPERATURE("temp"),
    // Jring combined-measurement metrics. Each `key` is the measurements.kindRaw value, so the
    // exporter's generic measurement path picks them up; a new otlp_export_state cursor row is
    // created per key on first export (no migration needed).
    BLOOD_PRESSURE_SYSTOLIC("bp_sys"),
    BLOOD_PRESSURE_DIASTOLIC("bp_dia"),
    BLOOD_SUGAR("glucose"),
    FATIGUE("fatigue"),
    ACTIVITY_DAILY("activity_daily"),
    SLEEP("sleep"),
    WORKOUT("workout"),
    GPS("gps"),
    DEVICE("device");

    companion object {
        val MEASUREMENT_TYPES = listOf(
            HEART_RATE, SPO2, HRV, STRESS, TEMPERATURE,
            BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC, BLOOD_SUGAR, FATIGUE,
        )
    }
}

/**
 * Immutable snapshot of the export configuration, assembled from DataStore (non-secret) +
 * SecretStore (credentials) by [OtlpConfigStore].
 */
data class OtlpExportConfig(
    val enabled: Boolean,
    val endpoint: String,
    val authType: OtlpAuthType,
    val username: String?,
    val password: String?,
    val bearerToken: String?,
    val headerName: String?,
    val headerValue: String?,
    val includeGps: Boolean,
    val wifiOnly: Boolean,
    val instanceId: String,
    val deviceModel: String?,
    val appVersion: String,
) {
    val isConfigured: Boolean get() = endpoint.isNotBlank()

    val isHttps: Boolean get() = endpoint.trim().startsWith("https://", ignoreCase = true)

    /**
     * The OTLP metrics URL. If the user already typed a full path ending in `/v1/metrics`
     * (e.g. VictoriaMetrics `…/opentelemetry/v1/metrics`) it is used verbatim; otherwise we
     * append `/v1/metrics` to the base (covers Alloy/Collector and Grafana Cloud `…/otlp`).
     */
    val metricsUrl: String
        get() {
            val base = endpoint.trim().trimEnd('/')
            return if (base.endsWith("/v1/metrics")) base else "$base/v1/metrics"
        }
}
