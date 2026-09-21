package ca.stewark.helioflux.ui.solaractivity

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarImagerySelectorTest {
    @Test
    fun imagerySelectorUsesApprovedOrderAndLabels() {
        assertEquals(
            listOf(
                SolarGalleryItem.Hmi,
                SolarGalleryItem.LascoC2,
                SolarGalleryItem.LascoC3,
                SolarGalleryItem.Enlil,
            ),
            SolarGalleryItem.entries,
        )
        assertEquals(
            listOf("HMI", "C2", "C3", "ENLIL"),
            SolarGalleryItem.entries.map { it.selectorLabel },
        )
        assertEquals(SolarGalleryItem.Hmi, DefaultSolarGalleryItem)
    }

    @Test
    fun oldCarouselAndTwoByTwoGridAreRemoved() {
        val source =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryGallery.kt")
                .readText()

        assertFalse(source.contains("LazyRow"))
        assertFalse(source.contains("chunked(2)"))
        assertTrue(source.contains("solar-imagery-selector"))
    }

    @Test
    fun enlilCardDelegatesAnimatedStageToReusablePlayer() {
        val source =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt")
                .readText()

        assertTrue(source.contains("fun EnlilAnimation("))
        assertTrue(source.contains("EnlilAnimation("))
    }
}
