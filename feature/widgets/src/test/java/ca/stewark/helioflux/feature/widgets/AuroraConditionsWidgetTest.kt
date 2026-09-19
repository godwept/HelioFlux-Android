package ca.stewark.helioflux.feature.widgets

import ca.stewark.helioflux.core.model.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuroraConditionsWidgetTest {
    @Test
    fun fixtureExposesKpOvationPowerAndFreshness() {
        val model = AuroraConditionsWidgetModel("Kp 5.0", "OVATION max 72%", "North 48 GW", DataFreshness.Fresh)
        assertEquals("Kp 5.0", model.kp)
        assertEquals("OVATION max 72%", model.ovationSummary)
        assertEquals("North 48 GW", model.hemisphericPower)
        assertEquals("LIVE", freshnessLabel(model.freshness))
    }

    @Test
    fun hemisphericPowerMayBeAbsent() {
        assertNull(AuroraConditionsWidgetModel("Kp 2.0", "OVATION max 18%", null, DataFreshness.Cached).hemisphericPower)
    }

    @Test
    fun widgetUsesResponsiveSizeMode() {
        assert(AuroraConditionsWidget().sizeMode is androidx.glance.appwidget.SizeMode.Responsive)
    }
}
