package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AuroraModelsTest {
    @Test
    fun `aurora point preserves location and intensity`() {
        val point = AuroraPoint(
            latitude = 52.5,
            longitude = -67.25,
            intensity = 38.75,
        )

        assertEquals(52.5, point.latitude, 0.0)
        assertEquals(-67.25, point.longitude, 0.0)
        assertEquals(38.75, point.intensity, 0.0)
    }

    @Test
    fun `aurora snapshot keeps observation forecast and filtered points together`() {
        val points = listOf(
            AuroraPoint(55.0, -70.0, 45.0),
            AuroraPoint(56.0, -69.0, 50.0),
        )
        val snapshot = AuroraSnapshot(
            observationTimestampMillis = 1_725_000_000_000L,
            forecastTimestampMillis = 1_725_000_300_000L,
            points = points,
        )

        assertEquals(1_725_000_000_000L, snapshot.observationTimestampMillis)
        assertEquals(1_725_000_300_000L, snapshot.forecastTimestampMillis)
        assertEquals(points, snapshot.points)
    }
}
