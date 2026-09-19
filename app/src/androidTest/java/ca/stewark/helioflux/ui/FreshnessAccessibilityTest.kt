package ca.stewark.helioflux.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.ui.components.FreshnessIndicator
import org.junit.Rule
import org.junit.Test

class FreshnessAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun collapsedIndicatorRetainsFreshnessAndLastUpdatedContext() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme {
                FreshnessIndicator(
                    freshness = DataFreshness.Cached,
                    labelDurationMillis = 100,
                    lastUpdatedContext = "Updated 12 min ago",
                )
            }
        }
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()

        rule.onNodeWithTag("freshness-indicator")
            .assertContentDescriptionEquals("Data freshness: Cached. Updated 12 min ago")
    }
}
