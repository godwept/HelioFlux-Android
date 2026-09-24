package ca.stewark.helioflux.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionHeadingSourceTest {
    @Test
    fun homeForecastKeepsSharedHeadingAfterCurrentConditionsTitleRemoval() {
        val current = File(
            "src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt",
        ).readText()
        val forecast = File(
            "src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt",
        ).readText()

        assertFalse(current.contains("HelioFluxSectionHeading("))
        assertTrue(
            forecast.contains(
                "HelioFluxSectionHeading(\"NOAA Forecast\", topSpacing = 0.dp)",
            ),
        )
        assertFalse(current.contains("Text(\"Current Conditions\""))
        assertFalse(forecast.contains("Text(\"NOAA Forecast\""))
    }
}
