package ca.stewark.helioflux.core.data.network

import ca.stewark.helioflux.core.data.parser.AceEpamParser
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.fail
import org.junit.Test

class AceEpamLiveProbeTest {
    @Test
    fun `live LaTiS request returns parseable recent EPAM rows`() {
        val end = System.currentTimeMillis()
        val start = end - 72L * 60L * 60L * 1_000L
        val url = HelioFluxEndpoints.aceEpam(start, end)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000

        val status = connection.responseCode
        val body =
            (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

        if (status !in 200..299) {
            fail("LaTiS HTTP " + status + " for " + url + "\n" + body.take(2000))
        }

        val rows =
            try {
                AceEpamParser.parse(body)
            } catch (error: Exception) {
                fail("LaTiS parser failed for " + url + "\n" + body.take(2000) + "\n" + error)
                emptyList()
            }

        if (rows.isEmpty()) {
            fail("LaTiS returned no parseable EPAM rows for " + url + "\n" + body.take(2000))
        }
    }
}
