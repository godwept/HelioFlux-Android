package ca.stewark.helioflux.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.core.model.FlareEvent
import ca.stewark.helioflux.core.model.FlareProbabilities
import ca.stewark.helioflux.ui.solaractivity.SolarActivityScreen
import ca.stewark.helioflux.ui.solaractivity.SolarActivityUiState
import org.junit.Rule
import org.junit.Test

class PartialDataTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun mixedSectionStatesDoNotBecomeScreenWideError() {
        val failed = DataSourceKey("failed")
        val empty = DataSourceKey("empty")
        val cached = DataSourceKey("cached")
        val fresh = DataSourceKey("fresh")
        rule.setContent {
            SolarActivityScreen(
                state =
                    SolarActivityUiState(
                        probabilities =
                            RepositoryState.Available(
                                FlareProbabilities(25, 5, 1),
                                fresh,
                                DataFreshness.Fresh,
                            ),
                        flares =
                            RepositoryState.Failure(
                                failed,
                                "source unavailable",
                                listOf(FlareEvent("flare-1", "M1.0", 1L, "GOES", null, null)),
                                DataFreshness.Cached,
                            ),
                        cmes = RepositoryState.Empty(empty, DataFreshness.Fresh),
                        epam = RepositoryState.Failure(cached, "offline", emptyList(), DataFreshness.Cached),
                    ),
                expanded = false,
            )
        }

        rule.onNodeWithText("Solar Imagery").assertIsDisplayed()
        rule.onNodeWithText("25%").assertIsDisplayed()
        rule.onNodeWithTag("solar-activity-compact")
            .performScrollToNode(hasTestTag("recent-events-row"))
        rule.onNodeWithTag("recent-events-row").assertExists()
        rule.onNodeWithTag("recent-flares-card").assertExists()
        rule.onNodeWithTag("recent-cmes-card").assertExists()
        rule.onNodeWithText("Showing cached flare data").assertExists()
        rule.onNodeWithText("No recent CMEs").assertExists()
        rule.onNodeWithTag("flare-row-flare-1").assertExists()
    }
}
