package space.linuxct.pulseloop.data.export.otlp

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest
import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.InstrumentationScope
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.metrics.v1.Gauge
import io.opentelemetry.proto.metrics.v1.Metric
import io.opentelemetry.proto.metrics.v1.NumberDataPoint
import io.opentelemetry.proto.metrics.v1.ResourceMetrics
import io.opentelemetry.proto.metrics.v1.ScopeMetrics
import io.opentelemetry.proto.resource.v1.Resource

/**
 * Serializes [OtlpPoint]s into an OTLP `ExportMetricsServiceRequest` protobuf payload.
 * One ResourceMetrics (constant resource identity) → one ScopeMetrics → one Metric per
 * (name, unit), each a Gauge whose NumberDataPoints carry the capture timestamp + bounded
 * per-point attributes. `timeUnixNano` is a protobuf fixed64 (8 LE bytes) — no int64-as-string
 * concern, that only affects the JSON encoding we deliberately avoid.
 */
object OtlpProtoSerializer {

    fun serialize(config: OtlpExportConfig, points: List<OtlpPoint>): ByteArray {
        val resource = Resource.newBuilder()
            .addAttributes(strAttr("service.name", "pulseloop"))
            .addAttributes(strAttr("service.version", config.appVersion))
            .addAttributes(strAttr("service.instance.id", config.instanceId))
            .apply { config.deviceModel?.let { addAttributes(strAttr("device.model", it)) } }
            .build()

        val scope = ScopeMetrics.newBuilder().setScope(
            InstrumentationScope.newBuilder()
                .setName("pulseloop.export")
                .setVersion(config.appVersion)
                .build()
        )

        points.groupBy { it.metric to it.unit }.forEach { (key, group) ->
            val (name, unit) = key
            val gauge = Gauge.newBuilder()
            for (p in group) {
                val ndp = NumberDataPoint.newBuilder()
                    .setTimeUnixNano(p.timeUnixNano)
                    .setAsDouble(p.value)
                for ((k, v) in p.attributes) ndp.addAttributes(strAttr(k, v))
                gauge.addDataPoints(ndp.build())
            }
            scope.addMetrics(
                Metric.newBuilder()
                    .setName(name)
                    .setUnit(unit)
                    .setGauge(gauge.build())
                    .build()
            )
        }

        val resourceMetrics = ResourceMetrics.newBuilder()
            .setResource(resource)
            .addScopeMetrics(scope.build())
            .build()

        return ExportMetricsServiceRequest.newBuilder()
            .addResourceMetrics(resourceMetrics)
            .build()
            .toByteArray()
    }

    private fun strAttr(key: String, value: String): KeyValue =
        KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder().setStringValue(value).build())
            .build()
}
