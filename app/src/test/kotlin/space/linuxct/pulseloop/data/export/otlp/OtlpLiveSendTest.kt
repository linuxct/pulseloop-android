package space.linuxct.pulseloop.data.export.otlp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LIVE end-to-end send against a real OTLP/HTTP endpoint, using the exact production code path
 * (OtlpProtoSerializer + OtlpHttpSender). Hits the network, so it is NOT part of the normal unit
 * run — invoke explicitly with --tests. Endpoint + token are passed via system properties:
 *   ./gradlew :app:testDebugUnitTest --tests "*OtlpLiveSendTest" \
 *       -Dotlp.endpoint="https://…/otlp" -Dotlp.auth="Basic …"
 */
class OtlpLiveSendTest {

    @Test
    fun sendsRealMetrics() {
        val endpoint = System.getProperty("otlp.endpoint")
        val auth = System.getProperty("otlp.auth")
        if (endpoint.isNullOrBlank() || auth.isNullOrBlank()) {
            println("[otlp-live] skipped: -Dotlp.endpoint / -Dotlp.auth not set")
            return
        }

        val config = OtlpExportConfig(
            enabled = true,
            endpoint = endpoint,
            authType = OtlpAuthType.NONE,
            username = null,
            password = null,
            bearerToken = null,
            headerName = "Authorization",
            headerValue = auth,
            includeGps = false,
            wifiOnly = false,
            instanceId = "e2e-test",
            deviceModel = "jring",
            appVersion = "2.0.0-e2e",
        )

        val now = System.currentTimeMillis()
        fun nanos(msAgo: Long) = (now - msAgo) * 1_000_000L
        val ring = mapOf("source" to "ring")

        val points = buildList {
            // Backdated heart-rate series over the last ~90 minutes (proves capture-time export).
            add(OtlpPoint("health.heart_rate", "bpm", nanos(90 * 60_000), 58.0, ring))
            add(OtlpPoint("health.heart_rate", "bpm", nanos(60 * 60_000), 64.0, ring))
            add(OtlpPoint("health.heart_rate", "bpm", nanos(30 * 60_000), 71.0, ring))
            add(OtlpPoint("health.heart_rate", "bpm", nanos(10 * 60_000), 69.0, ring))
            add(OtlpPoint("health.heart_rate", "bpm", nanos(0), 66.0, ring))
            // SpO2 + a daily activity total + a workout summary + a sleep stage block.
            add(OtlpPoint("health.spo2", "%", nanos(5 * 60_000), 98.0, mapOf("source" to "manual")))
            add(OtlpPoint("health.steps.daily", "{steps}", nanos(0), 8421.0, mapOf("source" to "ring_history")))
            add(OtlpPoint("health.workout.duration", "min", nanos(45 * 60_000), 32.0, mapOf("activity_type" to "run")))
            add(OtlpPoint("health.sleep.stage.duration", "min", nanos(80 * 60_000), 25.0, mapOf("stage" to "deep")))
        }

        val payload = OtlpProtoSerializer.serialize(config, points)
        println("[otlp-live] POST ${config.metricsUrl}  (${points.size} points, ${payload.size} proto bytes)")

        val result = runBlocking { OtlpHttpSender().send(config, payload) }
        println("[otlp-live] result = $result")

        assertTrue(
            "expected a 2xx Success, got $result",
            result is SendResult.Success,
        )
        println("[otlp-live] OK — rejected data points: ${(result as SendResult.Success).rejected}")
    }
}
