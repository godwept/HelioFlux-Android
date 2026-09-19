package ca.stewark.helioflux.core.data.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpTransportTest {
    @Test
    fun `get returns status body and headers without leaking okhttp types`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(206).setBody("payload").addHeader("X-Test", "yes"))
            val result = OkHttpTransport(OkHttpClient()).get(server.url("/data").toString())
            assertEquals(206, result.status)
            assertEquals("payload", result.body)
            assertEquals(listOf("yes"), result.headers["X-Test"])
        }
    }

    @Test
    fun `head exposes last modified header`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).addHeader("Last-Modified", "Sat, 19 Sep 2026 12:00:00 GMT"))
            val result = OkHttpTransport(OkHttpClient()).head(server.url("/image").toString())
            assertEquals(listOf("Sat, 19 Sep 2026 12:00:00 GMT"), result.headers["Last-Modified"])
        }
    }
}
