package ca.stewark.helioflux.core.data.network

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import org.junit.Assert.fail
import org.junit.Test

class AceEpamLiveProbeTest {
    @Test
    fun `show live LaTiS EPAM dataset columns`() {
        val endMillis = System.currentTimeMillis()
        val startMillis = endMillis - 72L * 60L * 60L * 1_000L
        val end = Instant.ofEpochMilli(endMillis)
        val start = Instant.ofEpochMilli(startMillis)
        val base = "https://lasp.colorado.edu/space-weather-portal/latis/dap/"
        val datasets = listOf("ace_epam_5m", "iswa_ace_epam_P5M")

        datasets.forEach { dataset ->
            val url = base + dataset + ".csv?time%3E=" + start + "&time%3C=" + end + "&limit(3)"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            val status = connection.responseCode
            val body =
                (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            System.out.println("LATIS_DATASET=" + dataset)
            System.out.println("LATIS_URL=" + url)
            System.out.println("LATIS_STATUS=" + status)
            System.out.println("LATIS_BODY_START\\n" + body.take(8000) + "\\nLATIS_BODY_END")
        }

        fail("Diagnostic probe complete")
    }
}
