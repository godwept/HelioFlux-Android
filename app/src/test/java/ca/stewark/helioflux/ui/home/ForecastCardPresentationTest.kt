package ca.stewark.helioflux.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastCardPresentationTest {
    @Test fun collapsedForecastMatchesPwaClampAndMinimumHeight() {
        assertEquals(5, FORECAST_COLLAPSED_MAX_LINES)
        assertEquals(200, FORECAST_CARD_MIN_HEIGHT_DP)
    }
}
