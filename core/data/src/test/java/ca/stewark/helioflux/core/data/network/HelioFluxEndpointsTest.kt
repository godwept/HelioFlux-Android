package ca.stewark.helioflux.core.data.network

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class HelioFluxEndpointsTest {
    @Test
    fun `direct endpoints use NOAA host`() {
        listOf(
            HelioFluxEndpoints.rtswMag, HelioFluxEndpoints.rtswPlasma, HelioFluxEndpoints.kp,
            HelioFluxEndpoints.goesInstrumentSources, HelioFluxEndpoints.goesPrimaryMagnetometer,
            HelioFluxEndpoints.goesSecondaryMagnetometer, HelioFluxEndpoints.goesPrimaryXray,
            HelioFluxEndpoints.goesSecondaryXray, HelioFluxEndpoints.ovation,
            HelioFluxEndpoints.hemisphericPower, HelioFluxEndpoints.aceEpam,
            HelioFluxEndpoints.forecastDiscussion,
        ).forEach { assertEquals("services.swpc.noaa.gov", URI(it).host) }
    }

    @Test
    fun `ace epam uses historical five minute json feed`() {
        assertEquals(
            "https://services.swpc.noaa.gov/json/ace/epam/ace_epam_5m.json",
            HelioFluxEndpoints.aceEpam,
        )
    }

    @Test
    fun `protected endpoints use existing worker host`() {
        listOf(
            HelioFluxEndpoints.donki, HelioFluxEndpoints.helioviewer, HelioFluxEndpoints.hek,
            HelioFluxEndpoints.hmi, HelioFluxEndpoints.lasco, HelioFluxEndpoints.enlil,
        ).forEach { assertEquals("helioflux-api-proxy.mathew-stewart.workers.dev", URI(it).host) }
    }
}
