package ca.stewark.helioflux.ui.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetDeepLinksTest {
    @Test fun spaceWeatherWidgetOpensSpaceWeather() {
        val intent=Intent().putExtra(WidgetDeepLinks.EXTRA_DESTINATION,"space-weather")
        assertEquals(HelioFluxDestination.SpaceWeather,WidgetDeepLinks.destination(intent))
    }
    @Test fun auroraWidgetOpensSpaceWeatherFocusedOnGlobe() {
        val intent=Intent().putExtra(WidgetDeepLinks.EXTRA_DESTINATION,"space-weather").putExtra(WidgetDeepLinks.EXTRA_FOCUS,"aurora-globe")
        assertEquals(HelioFluxDestination.SpaceWeather,WidgetDeepLinks.destination(intent))
        assertEquals(WidgetDeepLinks.FOCUS_AURORA_GLOBE,WidgetDeepLinks.focus(intent))
    }
    @Test fun sunWidgetOpensHomeFocusedOnHero() {
        val intent=Intent().putExtra(WidgetDeepLinks.EXTRA_DESTINATION,"home").putExtra(WidgetDeepLinks.EXTRA_FOCUS,"sun-hero")
        assertEquals(HelioFluxDestination.Home,WidgetDeepLinks.destination(intent))
        assertEquals(WidgetDeepLinks.FOCUS_SUN_HERO,WidgetDeepLinks.focus(intent))
    }
}
