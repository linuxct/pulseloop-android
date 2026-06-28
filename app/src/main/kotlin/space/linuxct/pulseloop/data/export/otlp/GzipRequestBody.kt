package space.linuxct.pulseloop.data.export.otlp

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import okio.gzip

/** Wraps a [RequestBody] so it is gzipped on the wire; pair with a `Content-Encoding: gzip` header. */
class GzipRequestBody(private val delegate: RequestBody) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()

    // Unknown until compressed; chunked transfer is fine for OTLP receivers.
    override fun contentLength(): Long = -1

    override fun writeTo(sink: BufferedSink) {
        val gzipSink = sink.gzip().buffer()
        delegate.writeTo(gzipSink)
        gzipSink.close()
    }
}
