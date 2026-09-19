package ca.stewark.helioflux.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ca.stewark.helioflux.core.model.DataFreshness
import org.junit.Rule
import org.junit.Test

class FreshnessIndicatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dotAlwaysVisible_labelTimesOut_tapReveals_andSemanticsRemain() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                FreshnessIndicator(
                    freshness = DataFreshness.Fresh,
                    labelDurationMillis = 1_000,
                )
            }
        }

        composeRule.onNodeWithTag("freshness-dot").assertIsDisplayed()
        composeRule.onNodeWithTag("freshness-label").assertIsDisplayed()
        composeRule.onNodeWithTag("freshness-indicator")
            .assertContentDescriptionEquals("Data freshness: Fresh")

        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("freshness-dot").assertIsDisplayed()
        composeRule.onAllNodesWithTag("freshness-label").assertCountEquals(0)

        composeRule.onNodeWithTag("freshness-indicator").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("freshness-label").assertIsDisplayed()
    }

    @Test
    fun statusChange_revealsNewLabel() {
        var freshness = DataFreshness.Fresh
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                FreshnessIndicator(freshness = freshness, labelDurationMillis = 100)
            }
        }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.waitForIdle()

        freshness = DataFreshness.Cached
        composeRule.runOnIdle { }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("freshness-label").assertIsDisplayed()
        composeRule.onNodeWithTag("freshness-indicator")
            .assertContentDescriptionEquals("Data freshness: Cached")
    }
}
