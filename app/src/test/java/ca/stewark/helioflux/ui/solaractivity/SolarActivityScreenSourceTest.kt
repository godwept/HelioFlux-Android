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
        assertTrue(source.contains("HelioFluxSectionHeading(\"Particle Environment\""))
        assertFalse(source.contains("HelioFluxSectionHeading(\"Recent Flares\""))
        assertFalse(source.contains("HelioFluxSectionHeading(\"Recent CMEs\""))
    }

    @Test
    fun compactSolarActivityUsesLazyFeedAndExpandedUsesFixedSplit() {
        assertEquals(2, Regex("LazyColumn\\(").findAll(source).count())
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

    @Test
    fun solarActivityKeepsTopContentBelowStatusBar() {
        val insetCall =
            "windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))"

        assertEquals(
            2,
            source.windowed(insetCall.length).count { it == insetCall },
        )
    }

    @Test
    fun xrayHeadingAndChartAreGroupedIntoOneSection() {
        assertTrue(source.contains("fun XrayActivitySection("))
        assertTrue(source.contains("HelioFluxSectionHeading(\"X-Ray Activity\", topSpacing = 0.dp)"))
        assertTrue(source.contains("XraySection(state, chartDomain)"))
    }

    @Test
    fun recentEventsUseOnePairedCardRow() {
        assertTrue(source.contains("fun SolarEventCards("))
        assertTrue(source.contains("testTag(\"recent-events-row\")"))
        assertTrue(source.contains("testTag(\"recent-flares-card\")"))
        assertTrue(source.contains("testTag(\"recent-cmes-card\")"))
    }

    @Test
    fun eventCardsDoNotAddNestedScrolling() {
        assertEquals(2, Regex("LazyColumn\\(").findAll(source).count())
        assertFalse(source.contains("verticalScroll("))
    }
}
