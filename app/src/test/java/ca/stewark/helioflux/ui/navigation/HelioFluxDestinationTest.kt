package ca.stewark.helioflux.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class HelioFluxDestinationTest {
    @Test
    fun destinations_areExactlyTheThreeStableAppDestinations() {
        assertEquals(
            listOf(
                Triple("Home", "home", "Home"),
                Triple("SpaceWeather", "space-weather", "Space Weather"),
                Triple("SolarActivity", "solar-activity", "Solar Activity"),
            ),
            HelioFluxDestination.entries.map { Triple(it.name, it.route, it.label) },
        )
    }
}
