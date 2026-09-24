package ca.stewark.helioflux.ui.home

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenSourceTest {
    @Test
    fun expandedHomeUsesSpaceWeatherStyleSplitWeights() {
        assertEquals(0.43f, ExpandedHomeHeroWeight, 0.0f)
        assertEquals(0.57f, ExpandedHomeContentWeight, 0.0f)
    }

    @Test
    fun expandedMastheadAndHeroStayOutsideRightScrollPane() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt",
        ).readText()
        val expanded = source.substringAfter("private fun ExpandedHomeLayout(")
        val rightContent = source.substringAfter("private fun ExpandedHomeRightContent(")

        assertTrue(expanded.contains("HelioFluxMasthead()"))
        assertTrue(expanded.contains("home-expanded-hero"))
        assertTrue(expanded.contains("SolarHero("))
        assertTrue(expanded.contains("home-expanded-content"))
        assertTrue(expanded.contains("ExpandedHomeRightContent("))

        assertTrue(rightContent.contains("LazyColumn("))
        assertTrue(rightContent.contains("home-expanded-scroll"))
        assertTrue(rightContent.contains("CurrentConditions("))
        assertTrue(rightContent.contains("ForecastCards("))
        assertFalse(rightContent.contains("HelioFluxMasthead()"))
        assertFalse(rightContent.contains("SolarHero("))
    }
}
