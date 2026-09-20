package ca.stewark.helioflux.ui.home

import ca.stewark.helioflux.core.model.ForecastSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastCardPresentationTest {
    @Test fun shortForecastFitsWithoutExpansion() {
        val section = ForecastSection("particle", "Energetic Particle", "Summary", "No significant activity expected.", "Issued now")
        assertFalse(forecastNeedsExpansion(section))
        assertEquals(section.forecast, forecastCollapsedText(section))
    }

    @Test fun longForecastIsCappedAndCanExpand() {
        val forecast = buildString { repeat(30) { append("Extended solar activity forecast. ") } }
        val section = ForecastSection("solar", "Solar Activity", "Summary", forecast, "Issued now")
        assertTrue(forecastNeedsExpansion(section))
        assertTrue(forecastCollapsedText(section).endsWith("…"))
        assertTrue(forecastCollapsedText(section).length <= FORECAST_COLLAPSED_MAX_CHARS + 1)
    }
}
