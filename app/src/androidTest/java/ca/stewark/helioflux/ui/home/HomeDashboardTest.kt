package ca.stewark.helioflux.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import org.junit.Rule
import org.junit.Test

class HomeDashboardTest {
    @get:Rule val compose = createComposeRule()
    private val source = DataSourceKey("test")

    @Test fun cachedHeroAndConditionsRender() {
        val image = SolarImage(SolarImageType.Aia304, 1, "https://example.test/sun.png")
        val state = HomeUiState(
            frames = RepositoryState.Failure(source, "offline", listOf(image), DataFreshness.Cached),
            kp = RepositoryState.Available(listOf(KpSample(1, 5.0)), source, DataFreshness.Fresh),
        )
        compose.setContent { HomeScreen(state, false, {}) }
        compose.onNodeWithTag("solar-hero").assertExists()
        compose.onNodeWithText("Using cached solar imagery").assertExists()
        compose.onNodeWithTag("home-compact").assertExists()
        compose.onNodeWithText("5.0").assertExists()
    }

    @Test fun loadingHeroExposesLoadingIndicatorAndBlackStage() {
        compose.setContent { HomeScreen(HomeUiState(frames = RepositoryState.Loading), false, {}) }
        compose.onNodeWithTag("solar-hero").assertExists()
        compose.onNodeWithTag("solar-hero-stage").assertExists()
        compose.onNodeWithTag("solar-hero-loading").assertExists()
    }

    @Test fun homeRendersBrandedMastheadAndCompactComposition() {
        compose.setContent { HomeScreen(HomeUiState(), false, {}) }
        compose.onNodeWithTag("home-screen").assertExists()
        compose.onNodeWithTag("home-masthead").assertTextEquals("HELIOFLUX")
        compose.onNodeWithTag("home-compact").assertExists()
    }

    @Test fun metricTapRoutesAndExpandedLayoutIsSideBySide() {
        var target: HelioFluxDestination? = null
        compose.setContent { HomeScreen(HomeUiState(), true, { target = it }) }
        compose.onNodeWithTag("home-expanded").assertExists()
        compose.onNodeWithTag("metric-kp").performClick()
        compose.runOnIdle { assert(target == HelioFluxDestination.SpaceWeather) }
    }

    @Test fun compactConditionPillsKeepOnlySpaceWeatherMetrics() {
        var target: HelioFluxDestination? = null
        compose.setContent { HomeScreen(HomeUiState(), false, { target = it }) }
        compose.onNodeWithTag("condition-metrics").assertExists()
        compose.onNodeWithTag("metric-kp").assertExists().performClick()
        compose.runOnIdle { assert(target == HelioFluxDestination.SpaceWeather) }
        compose.onNodeWithTag("metric-bz").assertExists()
        compose.onNodeWithTag("metric-wind").assertExists()
        compose.onNodeWithTag("metric-flare").assertDoesNotExist()
    }

    @Test fun forecastsExpandAndShowIssueText() {
        val section = ForecastSection("solar", "Solar Activity", "Summary", "Forecast detail", "Issued now")
        val state = HomeUiState(forecast = RepositoryState.Available(listOf(section), source, DataFreshness.Fresh))
        compose.setContent { HomeScreen(state, false, {}) }
        compose.onNodeWithTag("forecast-section").assertExists()
        compose.onNodeWithTag("forecast-track").assertExists()
        compose.onNodeWithText("Summary").assertExists()
        compose.onNodeWithText("Forecast detail").assertDoesNotExist()
        compose.onNodeWithTag("forecast-solar").performClick()
        compose.onNodeWithText("Forecast detail").assertExists()
        compose.onNodeWithText("Issued now").assertExists()
    }

    @Test fun emptyForecastAndMissingConditionsKeepHomeStructure() {
        compose.setContent { HomeScreen(HomeUiState(), false, {}) }
        compose.onNodeWithTag("home-masthead").assertExists()
        compose.onNodeWithTag("solar-hero").assertExists()
        compose.onNodeWithTag("current-conditions").assertExists()
        compose.onNodeWithTag("forecast-section").assertExists()
        compose.onAllNodesWithText("—").assertCountEquals(3)
    }
}
