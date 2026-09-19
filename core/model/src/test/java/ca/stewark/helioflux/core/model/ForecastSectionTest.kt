package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastSectionTest {
    @Test
    fun `forecast section preserves PWA discussion fields`() {
        val section = ForecastSection("solar", "Solar Activity", "summary", "forecast", "2026 Sep 19 1200 UTC")
        assertEquals("solar", section.key)
        assertEquals("Solar Activity", section.title)
        assertEquals("summary", section.summary)
        assertEquals("forecast", section.forecast)
        assertEquals("2026 Sep 19 1200 UTC", section.issueTime)
    }
}
