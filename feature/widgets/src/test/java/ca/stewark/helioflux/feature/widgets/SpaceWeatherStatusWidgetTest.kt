package ca.stewark.helioflux.feature.widgets

import ca.stewark.helioflux.core.model.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceWeatherStatusWidgetTest {
    @Test
    fun fixtureContainsAllStatusValuesAndFreshness() {
        val model = SpaceWeatherWidgetModel(
            kp = "Kp 6.3",
            bz = "Bz -8.4 nT",
            speed = "612 km/s",
            flareStatus = "M 42% · X 8%",
            freshness = DataFreshness.Delayed,
        )

        assertEquals("Kp 6.3", model.kp)
        assertEquals("Bz -8.4 nT", model.bz)
        assertEquals("612 km/s", model.speed)
        assertEquals("M 42% · X 8%", model.flareStatus)
        assertEquals("DELAYED", freshnessLabel(model.freshness))
    }

    @Test
    fun widgetUsesResponsiveSizeMode() {
        assert(SpaceWeatherStatusWidget().sizeMode is androidx.glance.appwidget.SizeMode.Responsive)
    }
}
