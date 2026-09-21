package ca.stewark.helioflux.ui.solaractivity

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarActivityScreenSourceTest {
    private val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt")
            .readText()

    @Test
    fun solarActivityUsesSharedSectionHeadingsAndNoGenericPageTitle() {
        assertFalse(source.contains("Text(\"Solar Activity\""))
        assertTrue(source.contains("HelioFluxSectionHeading(\"Solar Imagery\""))
        assertTrue(source.contains("HelioFluxSectionHeading(\"X-Ray Activity\""))
        assertTrue(source.contains("HelioFluxSectionHeading(\"Recent Flares\""))
        assertTrue(source.contains("HelioFluxSectionHeading(\"Recent CMEs\""))
        assertTrue(source.contains("HelioFluxSectionHeading(\"Particle Environment\""))
    }

    @Test
    fun compactSolarActivityUsesLazyFeedAndExpandedUsesFixedSplit() {
        assertTrue(source.contains("LazyColumn("))
        assertFalse(source.contains("verticalScroll("))
        assertTrue(source.contains("ExpandedSolarImageryWeight"))
        assertTrue(source.contains("ExpandedSolarDataWeight"))
        assertTrue(source.contains("solar-activity-expanded-imagery"))
        assertTrue(source.contains("solar-activity-expanded-content"))
        assertTrue(source.contains("solar-activity-expanded-scroll"))
    }

    @Test
    fun expandedSolarActivityUsesApprovedSplitWeights() {
        assertEquals(0.43f, ExpandedSolarImageryWeight, 0.0f)
        assertEquals(0.57f, ExpandedSolarDataWeight, 0.0f)
    }
}
