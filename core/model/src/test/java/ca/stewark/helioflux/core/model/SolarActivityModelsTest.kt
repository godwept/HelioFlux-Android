package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SolarActivityModelsTest {
    @Test
    fun `flare optional region and location need no sentinels`() {
        val flare = FlareEvent("id", "M1.2", 1L, "GOES", null, null)
        assertNull(flare.region)
        assertNull(flare.location)
    }

    @Test
    fun `cme optional direction and angle need no sentinels`() {
        val cme = CmeEvent("id", 2L, 750.0, null, null, "C", null)
        assertNull(cme.halfAngle)
        assertNull(cme.direction)
    }

    @Test
    fun `flare probabilities preserve class percentages`() {
        assertEquals(FlareProbabilities(65, 30, 5), FlareProbabilities(c = 65, m = 30, x = 5))
    }
}
