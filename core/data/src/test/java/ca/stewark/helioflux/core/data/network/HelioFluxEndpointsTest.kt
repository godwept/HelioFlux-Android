package ca.stewark.helioflux.core.data.network

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelioFluxEndpointsTest {
    @Test
    fun `direct endpoints use NOAA host`() {
        listOf(
            HelioFluxEndpoints.rtswMag, HelioFluxEndpoints.rtswPlasma, HelioFluxEndpoints.kp,
            HelioFluxEndpoints.goesInstrumentSources, HelioFluxEndpoints.goesPrimaryMagnetometer,
            HelioFluxEndpoints.goesSecondaryMagnetometer, HelioFluxEndpoints.goesPrimaryXray,
            HelioFluxEndpoints.goesSecondaryXray, HelioFluxEndpoints.ovation,
            HelioFluxEndpoints.hemisphericPower, HelioFluxEndpoints.forecastDiscussion,
            HelioFluxEndpoints.solarRegionSummary,
        ).forEach { assertTrue(it.startsWith("https://services.swpc.noaa.gov/")) }
    }

    @Test
    fun `ace epam requests exact LaTiS ISWA range`() {
        val start = Instant.parse("2026-09-18T12:00:00Z").toEpochMilli()
        val end = Instant.parse("2026-09-21T12:00:00Z").toEpochMilli()

        val url = HelioFluxEndpoints.aceEpam(start, end)

        assertTrue(
            url.startsWith(
                "https://lasp.colorado.edu/space-weather-portal/latis/dap/iswa_ace_epam_P5M.csv?",
            ),
        )
        assertTrue(url.contains("time%3E=2026-09-18T12:00:00Z"))
        assertTrue(url.contains("time%3C=2026-09-21T12:00:00Z"))
    }

    @Test
    fun `hmi metadata uses bounded direct LaTiS color magnetogram query`() {
        val start = Instant.parse("2026-09-21T11:20:00Z").toEpochMilli()
        val end = Instant.parse("2026-09-22T11:20:00Z").toEpochMilli()

        val url = HelioFluxEndpoints.hmiMetadata(start, end)

        assertTrue(
            url.startsWith(
                "https://lasp.colorado.edu/space-weather-portal/latis/dap/" +
                    "iswa_sdo_aia_hmic_files.json?",
            ),
        )
        assertTrue(url.contains("time%3E=2026-09-21T11:20:00Z"))
        assertTrue(url.contains("time%3C=2026-09-22T11:20:00Z"))
        assertTrue(url.contains("takeRight(1)"))
        assertTrue(url.contains("project(time,url)"))
        assertFalse(url.contains("workers.dev"))
    }

    @Test
    fun `solar region summary is current NOAA text source`() {
        assertTrue(HelioFluxEndpoints.solarRegionSummary.endsWith("/text/srs.txt"))
        assertFalse(HelioFluxEndpoints.solarRegionSummary.contains("workers.dev"))
    }

    @Test
    fun `protected endpoints use existing worker host`() {
        listOf(
            HelioFluxEndpoints.donki, HelioFluxEndpoints.helioviewer,
            HelioFluxEndpoints.lasco, HelioFluxEndpoints.enlil,
        ).forEach { assertTrue(it.startsWith("https://helioflux-api-proxy.mathew-stewart.workers.dev/api")) }
    }
}
