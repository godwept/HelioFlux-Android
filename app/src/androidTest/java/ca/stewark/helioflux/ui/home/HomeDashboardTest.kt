package ca.stewark.helioflux.ui.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import org.junit.Rule
import org.junit.Test

class HomeDashboardTest {
    @get:Rule val compose = createComposeRule()
    private val source = DataSourceKey("test")

    private class MemorySolarViewStore(initial: HomeSolarViewState = HomeSolarViewState()) : HomeSolarViewStore {
        var view = initial
        override fun read() = view
        override fun save(view: HomeSolarViewState) { this.view = view }
    }

    @Test fun savedCompactZoomShowsOneBackgroundBehindContent() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f, 0.2f, -0.1f))
        compose.setContent { HomeScreen(HomeUiState(), false, {}, viewStore = store) }
        compose.onNodeWithTag("solar-hero-background").assertExists()
        compose.onAllNodesWithTag("solar-hero-stage").assertCountEquals(1)
        compose.onNodeWithTag("home-masthead").assertExists()
        compose.onNodeWithTag("condition-metrics").assertExists()
        compose.onNodeWithTag("forecast-section").assertExists()
    }

    @Test fun pinchingSquareHeroEntersBackgroundMode() {
        val store = MemorySolarViewStore()
        compose.setContent { HomeScreen(HomeUiState(), false, {}, viewStore = store) }
        compose.onNodeWithTag("solar-hero-stage").performTouchInput {
            pinch(
                start0 = center + Offset(-width * 0.12f, 0f),
                end0 = center + Offset(-width * 0.32f, 0f),
                start1 = center + Offset(width * 0.12f, 0f),
                end1 = center + Offset(width * 0.32f, 0f),
            )
        }
        compose.onNodeWithTag("solar-hero-background").assertExists()
        compose.waitUntil(5_000) { store.view.scale > 1f }
    }

    @Test fun doubleTapZoomedBackgroundRestoresSquareAndSavesReset() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f, 0.2f, 0.1f))
        compose.setContent { HomeScreen(HomeUiState(), false, {}, viewStore = store) }
        compose.onNodeWithTag("solar-hero-background").performTouchInput {
            doubleClick(Offset(width * 0.94f, height * 0.8f))
        }
        compose.waitUntil(5_000) { store.view == HomeSolarViewState() }
        compose.onNodeWithTag("solar-hero-background").assertDoesNotExist()
        compose.onNodeWithTag("solar-hero-stage").assertExists()
    }

    @Test fun zoomedForecastCardRemainsClickable() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f))
        val section = ForecastSection("solar", "Solar Activity", "Summary", "Forecast detail", "Issued now")
        val state = HomeUiState(forecast = RepositoryState.Available(listOf(section), source, DataFreshness.Fresh))
        compose.setContent { HomeScreen(state, false, {}, viewStore = store) }
        compose.onNodeWithTag("forecast-solar").performClick()
        compose.onNodeWithText("TAP TO COLLAPSE").assertExists()
    }

    @Test fun exposedBackgroundDragUpdatesSavedPosition() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f))
        compose.setContent { HomeScreen(HomeUiState(), false, {}, viewStore = store) }
        compose.onNodeWithTag("home-screen").performTouchInput {
            swipe(
                start = Offset(width * 0.94f, height * 0.72f),
                end = Offset(width * 0.94f, height * 0.82f),
            )
        }
        compose.waitUntil(5_000) { store.view.panFractionY > 0f }
    }

    @Test fun savedExpandedZoomKeepsRightPaneScrollable() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f))
        compose.setContent { HomeScreen(HomeUiState(), true, {}, viewStore = store) }
        compose.onNodeWithTag("solar-hero-background").assertExists()
        compose.onNodeWithTag("home-expanded-content").assertExists()
        compose.onNodeWithTag("home-expanded-scroll").performScrollToNode(hasTestTag("forecast-section"))
        compose.onNodeWithTag("forecast-section").assertExists()
    }

    @Test fun savedBackgroundKeepsForegroundVisibleWhileImageryLoads() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f))
        compose.setContent {
            HomeScreen(HomeUiState(frames = RepositoryState.Loading), false, {}, viewStore = store)
        }
        compose.onNodeWithTag("solar-hero-background").assertExists()
        compose.onNodeWithTag("solar-hero-loading").assertExists()
        compose.onNodeWithTag("home-masthead").assertExists()
        compose.onNodeWithTag("forecast-section").assertExists()
    }

    @Test fun savedBackgroundRetainsCachedImageryNotice() {
        val store = MemorySolarViewStore(HomeSolarViewState(2f))
        val frame = SolarImage(SolarImageType.Aia304, 1, "https://example.test/sun.png")
        val state = HomeUiState(
            frames = RepositoryState.Failure(source, "offline", listOf(frame), DataFreshness.Cached),
        )
        compose.setContent { HomeScreen(state, false, {}, viewStore = store) }
        compose.onNodeWithTag("solar-hero-background").assertExists()
        compose.onNodeWithText("Using cached solar imagery").assertExists()
        compose.onNodeWithTag("forecast-section").assertExists()
    }

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

    @Test fun loadingHeroShowsOnlySpinnerAndLoadingText() {
        compose.setContent { HomeScreen(HomeUiState(frames = RepositoryState.Loading), false, {}) }
        compose.onNodeWithTag("solar-hero").assertExists()
        compose.onNodeWithTag("solar-hero-stage").assertExists()
        compose.onNodeWithTag("solar-hero-loading").assertExists()
        compose.onNodeWithText("Loading...").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("solar-frame-0"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun homeRendersBrandedMastheadAndCompactComposition() {
        compose.setContent { HomeScreen(HomeUiState(), false, {}) }
        compose.onNodeWithTag("home-screen").assertExists()
        compose.onNodeWithTag("home-masthead").assertExists()
        compose.onAllNodesWithText("HELIOFLUX").assertCountEquals(3)
        compose.onNodeWithTag("home-compact").assertExists()
        compose.onNodeWithTag("solar-hero-stage").assertExists()
        compose.onNodeWithTag("condition-metrics").assertExists()
        compose.onNodeWithTag("forecast-section").assertExists()
    }

    @Test fun metricTapRoutesFromExpandedRightPane() {
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

    @Test fun currentConditionsRendersOnlyMetricPills() {
        compose.setContent { HomeScreen(HomeUiState(), false, {}) }

        compose.onNodeWithTag("condition-metrics").assertExists()
        compose.onNodeWithTag("metric-kp").assertExists()
        compose.onNodeWithTag("metric-bz").assertExists()
        compose.onNodeWithTag("metric-wind").assertExists()
        compose.onNodeWithText("Current Conditions").assertDoesNotExist()
        compose.onNodeWithText("Aurora / geomagnetic status unavailable").assertDoesNotExist()
        compose.onNodeWithText("Geomagnetic storm conditions").assertDoesNotExist()
        compose.onNodeWithText("Geomagnetic conditions below storm level").assertDoesNotExist()
    }

    @Test fun expandedHomePinsHeroAndScrollsRightContent() {
        compose.setContent { HomeScreen(HomeUiState(), true, {}) }

        compose.onNodeWithTag("home-screen").assertExists()
        compose.onNodeWithTag("home-masthead").assertExists()
        compose.onNodeWithTag("home-expanded").assertExists()
        compose.onNodeWithTag("home-expanded-hero").assertExists()
        compose.onNodeWithTag("home-expanded-content").assertExists()
        compose.onNodeWithTag("home-expanded-scroll").assertExists()
        compose.onNodeWithTag("condition-metrics").assertExists()
        compose.onNodeWithTag("home-expanded-scroll").performScrollToNode(hasTestTag("forecast-section"))
        compose.onNodeWithTag("forecast-section").assertExists()
    }

    @Test fun shortForecastUsesAvailableCardRoomWithoutExpandAffordance() {
        val section = ForecastSection("solar", "Solar Activity", "Summary", "Forecast detail", "Issued now")
        val state = HomeUiState(forecast = RepositoryState.Available(listOf(section), source, DataFreshness.Fresh))
        compose.setContent { HomeScreen(state, false, {}) }
        compose.onNodeWithTag("forecast-section").assertExists()
        compose.onNodeWithTag("forecast-track").assertExists()
        compose.onNodeWithText("Summary").assertExists()
        compose.onNodeWithText("Forecast detail").assertExists()
        compose.onNodeWithText("TAP TO EXPAND").assertIsDisplayed()
        compose.onNodeWithText("Issued now").assertExists()
    }

    @Test fun longForecastOffersExpansion() {
        val longForecast = buildString { repeat(30) { append("Extended solar activity forecast. ") } }
        val section = ForecastSection("solar", "Solar Activity", "Summary", longForecast, "Issued now")
        val state = HomeUiState(forecast = RepositoryState.Available(listOf(section), source, DataFreshness.Fresh))
        compose.setContent { HomeScreen(state, false, {}) }
        compose.onNodeWithText("TAP TO EXPAND").assertIsDisplayed()
        compose.onNodeWithText("Issued now").assertIsDisplayed()
        val collapsedHeight = compose.onNodeWithTag("forecast-solar").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithTag("forecast-solar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("TAP TO COLLAPSE").assertIsDisplayed()
        compose.onNodeWithText("Issued now").assertIsDisplayed()
        val expandedHeight = compose.onNodeWithTag("forecast-solar").fetchSemanticsNode().boundsInRoot.height
        assert(expandedHeight > collapsedHeight)
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
