package ca.stewark.helioflux.imageloading

import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

class ImageDownloadProgressTest {
    @Test
    fun registryPublishesProgressByUrl() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c2small.gif"

            registry.update(url, bytesRead = 8_420L * 1024L, totalBytes = 17_860L * 1024L)

            assertEquals(
                ImageDownloadProgress(
                    bytesRead = 8_420L * 1024L,
                    totalBytes = 17_860L * 1024L,
                ),
                registry.observe(url).first(),
            )
        }

    @Test
    fun registryKeepsUrlsIndependentAndClearsOnlyRequestedUrl() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            registry.update("c2", 1024L, 2048L)
            registry.update("c3", 4096L, null)

            registry.clear("c2")

            assertNull(registry.observe("c2").first())
            assertEquals(ImageDownloadProgress(4096L, null), registry.observe("c3").first())
        }

    @Test
    fun registryTreatsInvalidTotalAsUnknown() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            registry.update("c2", 1024L, -1L)

            assertEquals(ImageDownloadProgress(1024L, null), registry.observe("c2").first())
        }

    @Test
    fun onlySmallLascoGifUrlsAreTracked() {
        assertTrue(isTrackedSolarActivityDownload("https://example.test/LATEST/current_c2small.gif"))
        assertTrue(isTrackedSolarActivityDownload("https://example.test/LATEST/current_c3small.gif"))
        assertFalse(isTrackedSolarActivityDownload("https://example.test/hmi.jpg"))
        assertFalse(isTrackedSolarActivityDownload("https://example.test/enlil/frame.jpg"))
    }

    @Test
    fun progressResponseBodyReportsCumulativeBytesAndKnownLength() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c2small.gif"
            val body =
                ProgressResponseBody(
                    url,
                    ChunkedBody(
                        bytes = ByteArray(4096),
                        reportedLength = 4096L,
                        maxChunkSize = 512L,
                    ),
                    registry,
                )
            val source = body.source()
            val sink = Buffer()

            source.read(sink, 512L)
            assertEquals(ImageDownloadProgress(512L, 4096L), registry.observe(url).first())

            sink.clear()
            source.read(sink, 512L)
            assertEquals(ImageDownloadProgress(1024L, 4096L), registry.observe(url).first())
        }

    @Test
    fun progressResponseBodyReportsUnknownLengthAsNull() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c3small.gif"
            val body =
                ProgressResponseBody(
                    url,
                    ChunkedBody(
                        bytes = ByteArray(2048),
                        reportedLength = -1L,
                        maxChunkSize = 512L,
                    ),
                    registry,
                )

            body.source().read(Buffer(), 512L)

            assertEquals(ImageDownloadProgress(512L, null), registry.observe(url).first())
        }

    @Test
    fun closingBeforeCompletionClearsProgress() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c2small.gif"
            val body =
                ProgressResponseBody(
                    url,
                    ChunkedBody(
                        bytes = ByteArray(4096),
                        reportedLength = 4096L,
                        maxChunkSize = 512L,
                    ),
                    registry,
                )
            val source = body.source()

            source.read(Buffer(), 512L)
            source.close()

            assertNull(registry.observe(url).first())
        }

    @Test
    fun readFailureClearsProgressAndRethrows() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c2small.gif"
            registry.update(url, 512L, 4096L)
            val body = ProgressResponseBody(url, FailingBody(), registry)

            try {
                body.source().read(Buffer(), 1024L)
                fail("Expected IOException")
            } catch (expected: IOException) {
                assertEquals("boom", expected.message)
            }

            assertNull(registry.observe(url).first())
        }

    @Test
    fun completedBodyRetainsFinalCountUntilMediaCallbackClearsIt() =
        runTest {
            val registry = ImageDownloadProgressRegistry()
            val url = "https://example.test/c2small.gif"
            val body = ProgressResponseBody(url, ByteArray(2048).toResponseBody(), registry)
            val source = body.source()
            val sink = Buffer()

            while (source.read(sink, 4096L) != -1L) {
                sink.clear()
            }
            source.close()

            assertEquals(ImageDownloadProgress(2048L, 2048L), registry.observe(url).first())
        }

    private class ChunkedBody(
        bytes: ByteArray,
        private val reportedLength: Long,
        private val maxChunkSize: Long,
    ) : ResponseBody() {
        private val upstream = Buffer().write(bytes)

        override fun contentType(): MediaType? = null

        override fun contentLength(): Long = reportedLength

        override fun source(): BufferedSource =
            object : ForwardingSource(upstream) {
                override fun read(
                    sink: Buffer,
                    byteCount: Long,
                ): Long = super.read(sink, minOf(byteCount, maxChunkSize))
            }.buffer()
    }

    private class FailingBody : ResponseBody() {
        override fun contentType(): MediaType? = null
        override fun contentLength(): Long = 4096L
        override fun source(): BufferedSource =
            object : ForwardingSource(Buffer().writeUtf8("data")) {
                override fun read(
                    sink: Buffer,
                    byteCount: Long,
                ): Long = throw IOException("boom")
            }.buffer()
    }
}
