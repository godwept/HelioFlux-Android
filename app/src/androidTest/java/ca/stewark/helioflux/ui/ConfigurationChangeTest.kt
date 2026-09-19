package ca.stewark.helioflux.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.window.core.layout.WindowSizeClass
import org.junit.Rule
import org.junit.Test

class ConfigurationChangeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectedDestinationSurvivesCompactToExpandedResize() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            HelioFluxApp(
                windowSizeClass = WindowSizeClass.compute(if (expanded) 900f else 400f, 800f),
            )
        }
        rule.onNodeWithText("Space Weather").performClick()
        rule.onNodeWithTag("destination-space-weather").assertIsDisplayed()

        rule.runOnIdle { expanded = true }

        rule.onNodeWithTag("helioflux-navigation-rail").assertIsDisplayed()
        rule.onNodeWithTag("destination-space-weather").assertIsDisplayed()
    }
}
