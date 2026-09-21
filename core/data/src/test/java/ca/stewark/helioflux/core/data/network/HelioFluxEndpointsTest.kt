package ca.stewark.helioflux.core.data.network

import java.time.Instant
import org.junit.Assert.assertEquals
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
        ).forEach { assertTrue(it.startsWith("https://services.swpc.noaa.gov/")) }
    }

    @Test
    fun `ace epam requests exact LaTiS range and NOAA proton product channels`() {
        val start = Instant.parse("2026-09-18T12:00:00Z").toEpochMilli()
        val end = Instant.parse("2026-09-21T12:00:00Z").toEpochMilli()

        val url = HelioFluxEndpoints.aceEpam(start, end)

        assertTrue(url.startsWith("https://lasp.colorado.edu/space-weather-portal/latis/dap/ace_epam_5m.csv?"))
        assertTrue(url.contains("time%3E=2026-09-18T12:00:00Z"))
        assertTrue(url.contains("time%3C=2026-09-21T12:00:00Z"))
        assertTrue(url.endsWith("project(time,p1,p3,p5,fp6p,p7)"))
    }

    @Test
    fun `protected endpoints use existing worker host`() {
        listOf(
            HelioFluxEndpoints.donki, HelioFluxEndpoints.helioviewer, HelioFluxEndpoints.hek,
            HelioFluxEndpoints.hmi, HelioFluxEndpoints.lasco, HelioFluxEndpoints.enlil,
        ).forEach { assertTrue(it.startsWith("https://helioflux-api-proxy.mathew-stewart.workers.dev/api")) }
    }
}
