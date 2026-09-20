package ca.stewark.helioflux.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastCardPresentationTest {
    @Test fun collapsedForecastUsesBoundedVisualLines() {
        assertEquals(7, FORECAST_COLLAPSED_MAX_LINES)
        assertEquals(260, FORECAST_CARD_HEIGHT_DP)
    }
}
