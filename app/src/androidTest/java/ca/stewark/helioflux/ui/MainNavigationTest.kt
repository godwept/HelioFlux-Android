package ca.stewark.helioflux.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.window.core.layout.WindowSizeClass
import ca.stewark.helioflux.ui.home.HomeUiState
import ca.stewark.helioflux.ui.solaractivity.SolarActivityUiState
import ca.stewark.helioflux.ui.spaceweather.SpaceWeatherUiState
import org.junit.Rule
import org.junit.Test

class MainNavigationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun navigatesAcrossAllPrimaryDestinationsAndBackHome() {
        rule.setContent {
            HelioFluxApp(
                windowSizeClass = WindowSizeClass.compute(400f, 800f),
                homeState = HomeUiState(),
                spaceWeatherState = SpaceWeatherUiState(),
                solarActivityState = SolarActivityUiState(),
            )
        }

        rule.onNodeWithText("HelioFlux").assertIsDisplayed()
        rule.onNodeWithText("Space Weather").performClick()
        rule.onNodeWithText("Space Weather", useUnmergedTree = true).assertIsSelected()
        rule.onNodeWithText("Solar Activity").performClick()
        rule.onNodeWithText("Solar Activity", useUnmergedTree = true).assertIsSelected()
        rule.onNodeWithText("Home").performClick()
        rule.onNodeWithText("Home", useUnmergedTree = true).assertIsSelected()
        rule.onNodeWithText("HelioFlux").assertIsDisplayed()
    }
}
