package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageryModelsTest {
    @Test
    fun `solar image preserves type timestamp and url`() {
        val image = SolarImage(SolarImageType.Aia304, 10L, "https://example.test/aia.png")
        assertEquals(SolarImageType.Aia304, image.type)
        assertEquals(10L, image.sourceTimestampMillis)
        assertEquals("https://example.test/aia.png", image.url)
    }

    @Test
    fun `active region preserves helioprojective coordinates`() {
        val region = ActiveRegion("14201", "4201", -125.5, 310.25)
        assertEquals(-125.5, region.helioprojectiveX, 0.0)
        assertEquals(310.25, region.helioprojectiveY, 0.0)
    }

    @Test
    fun `enlil frame keeps run and frame timestamps separate`() {
        val frame = EnlilFrame(100L, 200L, "https://example.test/enlil.png")
        assertEquals(100L, frame.runTimestampMillis)
        assertEquals(200L, frame.frameTimestampMillis)
    }
}
