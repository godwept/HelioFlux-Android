package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SolarWindModelsTest {
    @Test
    fun `magnetic sample preserves timestamp values and missing fields`() {
        val sample = SolarWindMag(
            timestampMillis = 1_725_000_000_000L,
            bx = 3.25,
            by = null,
            bz = -4.75,
            bt = 6.125,
        )

        assertEquals(1_725_000_000_000L, sample.timestampMillis)
        assertEquals(3.25, sample.bx!!, 0.0)
        assertNull(sample.by)
        assertEquals(-4.75, sample.bz!!, 0.0)
        assertEquals(6.125, sample.bt!!, 0.0)
    }

    @Test
    fun `plasma sample preserves timestamp values and missing fields`() {
        val sample = SolarWindPlasma(
            timestampMillis = 1_725_000_120_000L,
            density = 7.5,
            speed = 412.25,
            temperature = null,
        )

        assertEquals(1_725_000_120_000L, sample.timestampMillis)
        assertEquals(7.5, sample.density!!, 0.0)
        assertEquals(412.25, sample.speed!!, 0.0)
        assertNull(sample.temperature)
    }
}
