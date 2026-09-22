package ca.stewark.helioflux.imageloading

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

internal data class ImageDownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long?,
)

internal class ImageDownloadProgressRegistry {
    private val progressByUrl =
        MutableStateFlow<Map<String, ImageDownloadProgress>>(emptyMap())

    fun observe(url: String): Flow<ImageDownloadProgress?> =
        progressByUrl
            .map { it[url] }
            .distinctUntilChanged()

    fun update(
        url: String,
        bytesRead: Long,
        totalBytes: Long?,
    ) {
        val progress =
            ImageDownloadProgress(
                bytesRead = bytesRead.coerceAtLeast(0L),
                totalBytes = totalBytes?.takeIf { it > 0L },
            )
        progressByUrl.update { current ->
            current + (url to progress)
        }
    }

    fun clear(url: String) {
        progressByUrl.update { current ->
            if (url in current) current - url else current
        }
    }
}

internal val imageDownloadProgressRegistry = ImageDownloadProgressRegistry()

private fun isTrackedSolarActivityDownload(url: String): Boolean =
    url.endsWith("current_c2small.gif") ||
        url.endsWith("current_c3small.gif")

internal class ProgressResponseBody(
    private val url: String,
    private val delegate: ResponseBody,
    private val registry: ImageDownloadProgressRegistry,
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource {
        bufferedSource?.let { return it }

        val totalBytes = delegate.contentLength().takeIf { it > 0L }
        val forwarding =
            object : ForwardingSource(delegate.source()) {
                private var bytesRead = 0L
                private var completed = false

                override fun read(
                    sink: Buffer,
                    byteCount: Long,
                ): Long {
                    return try {
                        val read = super.read(sink, byteCount)
                        when {
                            read > 0L -> {
                                bytesRead += read
                                if (totalBytes != null && bytesRead >= totalBytes) {
                                    completed = true
                                }
                                registry.update(url, bytesRead, totalBytes)
                            }
                            read == -1L -> completed = true
                        }
                        read
                    } catch (error: Throwable) {
                        registry.clear(url)
                        throw error
                    }
                }

                override fun close() {
                    try {
                        super.close()
                    } finally {
                        if (!completed) {
                            registry.clear(url)
                        }
                    }
                }
            }

        return forwarding.buffer().also { bufferedSource = it }
    }
}

internal class ImageDownloadProgressInterceptor(
    private val registry: ImageDownloadProgressRegistry,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.toString()
        if (!isTrackedSolarActivityDownload(url)) {
            return chain.proceed(chain.request())
        }

        registry.clear(url)
        return try {
            val response = chain.proceed(chain.request())
            val body = response.body
            if (body == null) {
                registry.clear(url)
                response
            } else {
                response
                    .newBuilder()
                    .body(ProgressResponseBody(url, body, registry))
                    .build()
            }
        } catch (error: Throwable) {
            registry.clear(url)
            throw error
        }
    }
}
