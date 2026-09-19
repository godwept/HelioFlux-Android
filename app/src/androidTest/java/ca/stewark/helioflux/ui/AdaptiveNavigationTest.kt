package ca.stewark.helioflux.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.window.core.layout.WindowSizeClass
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class AdaptiveNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactWidth_showsBottomNavigation() {
        composeRule.setContent {
            HelioFluxApp(windowSizeClass = compactWindowSizeClass())
        }

        composeRule.onNodeWithTag("helioflux-bottom-navigation").assertIsDisplayed()
        composeRule.onNodeWithTag("helioflux-navigation-rail").assertDoesNotExist()
    }

    @Test
    fun expandedWidth_showsNavigationRail() {
        composeRule.setContent {
            HelioFluxApp(windowSizeClass = expandedWindowSizeClass())
        }

        composeRule.onNodeWithTag("helioflux-navigation-rail").assertIsDisplayed()
        composeRule.onNodeWithTag("helioflux-bottom-navigation").assertDoesNotExist()
    }

    @Test
    fun selectedDestination_survivesRecomposition() {
        composeRule.setContent {
            HelioFluxApp(windowSizeClass = compactWindowSizeClass())
        }

        composeRule.onNodeWithText("Space Weather").performClick()
        composeRule.onNodeWithTag("destination-space-weather").assertIsDisplayed()

        composeRule.runOnIdle { composeRule.mainClock.advanceTimeByFrame() }

        composeRule.onNodeWithTag("destination-space-weather").assertIsDisplayed()
    }

    private fun compactWindowSizeClass(): WindowSizeClass =
        WindowSizeClass.compute(
            dpWidth = 400f,
            dpHeight = 800f,
        )

    private fun expandedWindowSizeClass(): WindowSizeClass =
        WindowSizeClass.compute(
            dpWidth = 840f,
            dpHeight = 800f,
        )
}
