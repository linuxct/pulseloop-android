package space.linuxct.pulseloop.data.export.otlp

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the OTLP protobuf wire format end-to-end: serialize → parse back with the generated
 * classes. The critical assertion is that a BACKDATED capture timestamp survives exactly as a
 * fixed64 `time_unix_nano`, and that the bounded attributes / resource identity are correct.
 */
class OtlpProtoSerializerTest {

    private fun config() = OtlpExportConfig(
        enabled = true,
        endpoint = "https://otlp.example.com",
        authType = OtlpAuthType.NONE,
        username = null,
        password = null,
        bearerToken = null,
        headerName = null,
        headerValue = null,
        includeGps = false,
        wifiOnly = false,
        instanceId = "test-instance",
        deviceModel = "jring",
        appVersion = "2.0.0-test",
    )

    @Test
    fun backdatedPointRoundTrips() {
        val captureMs = 1_700_000_000_000L // a fixed point well in the past
        val point = OtlpPoint(
            metric = "health.heart_rate",
            unit = "bpm",
            timeUnixNano = captureMs * 1_000_000L,
            value = 72.0,
            attributes = mapOf("source" to "ring"),
        )

        val bytes = OtlpProtoSerializer.serialize(config(), listOf(point))
        val req = ExportMetricsServiceRequest.parseFrom(bytes)

        val rm = req.getResourceMetrics(0)
        assertTrue(
            "resource carries service.name=pulseloop",
            rm.resource.attributesList.any { it.key == "service.name" && it.value.stringValue == "pulseloop" },
        )
        assertTrue(
            "resource carries device.model",
            rm.resource.attributesList.any { it.key == "device.model" && it.value.stringValue == "jring" },
        )

        val metric = rm.getScopeMetrics(0).getMetrics(0)
        assertEquals("health.heart_rate", metric.name)
        assertEquals("bpm", metric.unit)
        assertTrue("metric is a gauge", metric.hasGauge())

        val dp = metric.gauge.getDataPoints(0)
        assertEquals("capture timestamp preserved exactly", captureMs * 1_000_000L, dp.timeUnixNano)
        assertEquals(72.0, dp.asDouble, 0.0001)
        assertTrue(
            "bounded source attribute present",
            dp.attributesList.any { it.key == "source" && it.value.stringValue == "ring" },
        )
        // Cardinality guard: value/timestamp must NOT leak into attributes.
        assertTrue(
            "no high-cardinality attributes",
            dp.attributesList.none { it.key in setOf("value", "timestamp", "measurement_id", "session_id") },
        )
    }

    @Test
    fun pointsGroupByMetricName() {
        val now = 1_700_000_000_000L * 1_000_000L
        val points = listOf(
            OtlpPoint("health.heart_rate", "bpm", now, 60.0, mapOf("source" to "ring")),
            OtlpPoint("health.heart_rate", "bpm", now + 1, 61.0, mapOf("source" to "ring")),
            OtlpPoint("health.spo2", "%", now, 98.0, mapOf("source" to "manual")),
        )
        val req = ExportMetricsServiceRequest.parseFrom(OtlpProtoSerializer.serialize(config(), points))
        val metrics = req.getResourceMetrics(0).getScopeMetrics(0).metricsList
        assertEquals("one Metric per distinct name", 2, metrics.size)
        val hr = metrics.first { it.name == "health.heart_rate" }
        assertEquals("hr data points grouped", 2, hr.gauge.dataPointsCount)
    }
}
