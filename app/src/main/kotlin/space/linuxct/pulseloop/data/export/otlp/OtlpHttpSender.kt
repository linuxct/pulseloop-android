package space.linuxct.pulseloop.data.export.otlp

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import space.linuxct.pulseloop.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

sealed interface SendResult {
    /** 2xx. [rejected] = OTLP partial_success.rejected_data_points (a 200 can still drop points). */
    data class Success(val rejected: Long) : SendResult
    /** Transient — worker should back off and retry. [retryAfterMs] honors a server `Retry-After`. */
    data class Retryable(val retryAfterMs: Long?) : SendResult
    /** 413 — caller should shrink the chunk and retry once. */
    data object PayloadTooLarge : SendResult
    /** Non-retryable: bad auth/endpoint/payload, or a rejected TLS connection. */
    data class Fatal(val code: Int, val message: String) : SendResult
}

/**
 * Posts an OTLP protobuf payload (gzipped) to the configured endpoint.
 * TLS posture is scheme-driven: `https://` uses [ConnectionSpec.MODERN_TLS] only (TLS 1.2/1.3,
 * strict cert validation, NO trust-all bypass — a bad cert surfaces as [SendResult.Fatal]);
 * `http://` uses [ConnectionSpec.CLEARTEXT] (the user's explicit choice is trusted).
 */
@Singleton
class OtlpHttpSender @Inject constructor() {

    private val protobufMedia = "application/x-protobuf".toMediaType()

    private val httpsClient by lazy { buildClient(listOf(ConnectionSpec.MODERN_TLS)) }
    private val cleartextClient by lazy { buildClient(listOf(ConnectionSpec.CLEARTEXT)) }

    private fun buildClient(specs: List<ConnectionSpec>): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectionSpecs(specs)
            .apply {
                // Debug-only: log the request line + headers (Authorization REDACTED) and the
                // response status so the exact URL/auth being sent can be inspected in logcat
                // (tag: okhttp.OkHttpClient). Never enabled in release builds.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                            redactHeader("Authorization")
                        }
                    )
                }
            }
            .build()

    suspend fun send(config: OtlpExportConfig, payload: ByteArray): SendResult = withContext(Dispatchers.IO) {
        val client = if (config.isHttps) httpsClient else cleartextClient
        val body = GzipRequestBody(payload.toRequestBody(protobufMedia))

        val builder = Request.Builder()
            .url(config.metricsUrl)
            .header("Content-Encoding", "gzip")
            .post(body)

        when (config.authType) {
            OtlpAuthType.BASIC ->
                if (!config.username.isNullOrBlank()) {
                    builder.header("Authorization", Credentials.basic(config.username.trim(), config.password?.trim() ?: ""))
                }
            OtlpAuthType.BEARER ->
                if (!config.bearerToken.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer ${config.bearerToken.trim()}")
                }
            OtlpAuthType.NONE -> {}
        }
        if (!config.headerName.isNullOrBlank() && !config.headerValue.isNullOrBlank()) {
            // Percent-decode the value (OTEL_EXPORTER_OTLP_HEADERS convention) so a header copied
            // verbatim from Grafana ("Basic%20<token>") becomes the real "Basic <token>".
            builder.header(config.headerName.trim(), percentDecode(config.headerValue.trim()))
        }

        try {
            client.newCall(builder.build()).execute().use { resp ->
                when {
                    resp.isSuccessful -> SendResult.Success(parseRejected(resp.body?.bytes()))
                    resp.code == 413 -> SendResult.PayloadTooLarge
                    resp.code in RETRYABLE_CODES -> SendResult.Retryable(parseRetryAfter(resp.header("Retry-After")))
                    resp.code >= 500 -> SendResult.Retryable(null)
                    else -> SendResult.Fatal(resp.code, resp.body?.string()?.take(300) ?: "HTTP ${resp.code}")
                }
            }
        } catch (e: SSLException) {
            // Strict TLS rejected the connection (bad/self-signed/expired cert). Do NOT retry/bypass.
            SendResult.Fatal(-1, "TLS error: ${e.message ?: "insecure connection rejected"}")
        } catch (e: IOException) {
            SendResult.Retryable(null)
        }
    }

    private fun parseRejected(bytes: ByteArray?): Long = try {
        if (bytes == null || bytes.isEmpty()) 0L
        else ExportMetricsServiceResponse.parseFrom(bytes).partialSuccess.rejectedDataPoints
    } catch (_: Exception) {
        0L
    }

    private fun parseRetryAfter(header: String?): Long? =
        header?.trim()?.toLongOrNull()?.let { it * 1000L }

    /**
     * Decodes %XX escapes (e.g. `%20` → space) per the OTEL_EXPORTER_OTLP_HEADERS convention.
     * Deliberately leaves `+` untouched — it is a valid base64 character, not an encoded space.
     */
    private fun percentDecode(value: String): String {
        if (!value.contains('%')) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 < value.length) {
                val code = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (code != null) {
                    out.append(code.toChar()); i += 3; continue
                }
            }
            out.append(c); i++
        }
        return out.toString()
    }

    private companion object {
        val RETRYABLE_CODES = setOf(408, 429, 502, 503, 504)
    }
}
