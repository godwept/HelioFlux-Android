package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParticleModelsTest {
    @Test
    fun `xray channels preserve chart gaps`() {
        val sample = XrayFluxSample(1L, 1e-7, null, 2e-7, null)
        assertEquals(1e-7, sample.goes18Short!!, 0.0)
        assertNull(sample.goes18Long)
        assertEquals(2e-7, sample.goes19Short!!, 0.0)
        assertNull(sample.goes19Long)
    }

    @Test
    fun `ace epam preserves fp6 and p7 proton channels`() {
        val sample = AceEpamSample(1L, 2.0, null, 3.0, null, 4.0, 5.0, null)
        assertEquals(2.0, sample.electronLow!!, 0.0)
        assertNull(sample.electronHigh)
        assertEquals(3.0, sample.protonLow!!, 0.0)
        assertNull(sample.protonMid)
        assertEquals(4.0, sample.protonHigh!!, 0.0)
        assertEquals(5.0, sample.protonFp6!!, 0.0)
        assertNull(sample.protonP7)
    }
}
