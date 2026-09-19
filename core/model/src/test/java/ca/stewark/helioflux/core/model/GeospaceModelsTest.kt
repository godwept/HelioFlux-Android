package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeospaceModelsTest {
    @Test
    fun `kp sample preserves timestamp and value`() {
        val sample = KpSample(1_725_000_000_000L, 4.67)

        assertEquals(1_725_000_000_000L, sample.timestampMillis)
        assertEquals(4.67, sample.kp!!, 0.0)
    }

    @Test
    fun `goes magnetometer sample represents arcjet gaps independently`() {
        val primaryGap = GoesMagSample(1_725_000_060_000L, null, 108.5)
        val secondaryGap = GoesMagSample(1_725_000_120_000L, 111.25, null)

        assertNull(primaryGap.primary)
        assertEquals(108.5, primaryGap.secondary!!, 0.0)
        assertEquals(111.25, secondaryGap.primary!!, 0.0)
        assertNull(secondaryGap.secondary)
    }

    @Test
    fun `hemispheric power sample preserves timestamp and hemisphere values`() {
        val sample = HemisphericPowerSample(
            timestampMillis = 1_725_000_180_000L,
            north = 42.5,
            south = 37.25,
        )

        assertEquals(1_725_000_180_000L, sample.timestampMillis)
        assertEquals(42.5, sample.north!!, 0.0)
        assertEquals(37.25, sample.south!!, 0.0)
    }
}
