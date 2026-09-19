package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuroraPaletteTest {
    @Test fun matchesPwaIntensityBands() {
        assertTrue(AuroraPalette.colorFor(4.99).isTransparent)
        assertEquals(AuroraRgba(90, 200, 250, 56), AuroraPalette.colorFor(5.0))
        assertEquals(AuroraRgba(0, 210, 190, 91), AuroraPalette.colorFor(9.0))
        assertEquals(AuroraRgba(52, 199, 89, 127), AuroraPalette.colorFor(16.0))
        assertEquals(AuroraRgba(255, 149, 0, 158), AuroraPalette.colorFor(26.0))
        assertEquals(AuroraRgba(255, 59, 48, 183), AuroraPalette.colorFor(41.0))
    }
}
