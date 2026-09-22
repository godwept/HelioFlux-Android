package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SolarActivityUiTest {
    @get:Rule val compose = createComposeRule()
    private val source = DataSourceKey("test")

    private fun populatedImageryState(): SolarActivityUiState =
        SolarActivityUiState(
            probabilities =
                RepositoryState.Available(
                    FlareProbabilities(25, 5, 1),
                    source,
                    DataFreshness.Fresh,
                ),
            magnetogram =
                RepositoryState.Available(
                    SolarImage(SolarImageType.Magnetogram, 1789907640000L, "https://example.test/hmi.jpg"),
                    source,
                    DataFreshness.Fresh,
                ),
            lascoC2 =
                RepositoryState.Available(
                    SolarImage(SolarImageType.LascoC2, 1789907640000L, "https://example.test/c2.gif"),
                    source,
                    DataFreshness.Fresh,
                ),
            lascoC3 =
                RepositoryState.Available(
                    SolarImage(SolarImageType.LascoC3, 1789907640000L, "https://example.test/c3.gif"),
                    source,
                    DataFreshness.Fresh,
                ),
            activeRegions =
                RepositoryState.Available(
                    listOf(
                        ActiveRegion("r1", "4532", 0.0, 0.0),
                        ActiveRegion("r2", "4533", 20.0, 20.0),
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
            enlil =
                RepositoryState.Available(
                    listOf(
                        EnlilFrame(1789900000000L, 1789900000000L, "https://example.test/e1.jpg"),
                        EnlilFrame(1789900000000L, 1789900000200L, "https://example.test/e2.jpg"),
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
        )

    private fun populatedEventState(): SolarActivityUiState =
        populatedImageryState().copy(
            flares =
                RepositoryState.Available(
                    listOf(
                        FlareEvent("flare-1", "M1.7", 1789907640000L, "GOES", "12345", "N12W34"),
                        FlareEvent("flare-2", "C4.2", 1789904040000L, "GOES", "12344", "N08E11"),
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
            cmes =
                RepositoryState.Available(
                    listOf(
                        CmeEvent(
                            "cme-1",
                            1789907640000L,
                            1250.0,
                            35.0,
                            "NW",
                            null,
                            "https://example.test/cme-1",
                        ),
                        CmeEvent(
                            "cme-2",
                            1789904040000L,
                            650.0,
                            25.0,
                            "W",
                            null,
                            null,
                        ),
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
        )

    @Test
    fun imagerySelectorStartsOnHmiAndSwitchesSources() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        compose.onNodeWithTag("solar-imagery-selector").assertExists()
        compose.onNodeWithTag("solar-imagery-stage-hmi").assertExists()
        compose.onNodeWithTag("solar-imagery-c2").performClick()
        compose.onNodeWithTag("solar-imagery-stage-c2").assertExists()
        compose.onNodeWithTag("solar-imagery-stage-hmi").assertDoesNotExist()
        compose.onNodeWithTag("solar-imagery-enlil").performClick()
        compose.onNodeWithTag("solar-imagery-stage-enlil").assertExists()
    }

    @Test
    fun compactLayoutExposesApprovedStructure() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        compose.onNodeWithTag("solar-activity-compact").assertExists()
        compose.onNodeWithText("Solar Imagery").assertExists()
        compose.onNodeWithTag("solar-activity-compact").performScrollToNode(hasText("Particle Environment"))
        compose.onNodeWithText("Particle Environment").assertExists()
    }

    @Test
    fun expandedLayoutPinsImageryAndScrollsSciencePane() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = true) }

        compose.onNodeWithTag("solar-activity-expanded").assertExists()
        compose.onNodeWithTag("solar-activity-expanded-imagery").assertExists()
        compose.onNodeWithTag("solar-activity-expanded-content").assertExists()
        compose.onNodeWithTag("solar-activity-expanded-scroll").assertExists()
        compose.onNodeWithTag("solar-activity-expanded-scroll").performScrollToNode(hasText("Particle Environment"))
        compose.onNodeWithTag("solar-activity-expanded-imagery").assertExists()
    }

    @Test
    fun compactXraySectionContainsProbabilityStrip() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        compose.onNodeWithTag("solar-activity-compact")
            .performScrollToNode(hasText("X-Ray Activity"))

        compose.onNodeWithText("X-Ray Activity").assertExists()
        compose.onNodeWithTag("flare-probability-strip").assertExists()
        compose.onNodeWithText("25%").assertExists()
    }

    @Test
    fun expandedXraySectionContainsProbabilityStrip() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = true) }

        compose.onNodeWithTag("solar-activity-expanded-scroll")
            .performScrollToNode(hasText("X-Ray Activity"))

        compose.onNodeWithText("X-Ray Activity").assertExists()
        compose.onNodeWithTag("flare-probability-strip").assertExists()
        compose.onNodeWithText("25%").assertExists()
    }

    @Test
    fun compactEventCardsKeepCompleteListsAndDetailsAction() {
        var openedCme: CmeEvent? = null
        compose.setContent {
            SolarActivityScreen(
                state = populatedEventState(),
                expanded = false,
                onCmeDetails = { openedCme = it },
            )
        }

        compose.onNodeWithTag("solar-activity-compact")
            .performScrollToNode(hasTestTag("recent-events-row"))

        compose.onNodeWithTag("recent-flares-card").assertExists()
        compose.onNodeWithTag("recent-cmes-card").assertExists()
        compose.onNodeWithTag("flare-row-flare-1").assertExists()
        compose.onNodeWithTag("flare-row-flare-2").assertExists()
        compose.onNodeWithTag("cme-row-cme-1").assertExists()
        compose.onNodeWithTag("cme-row-cme-2").assertExists()
        compose.onNodeWithText("Details").performClick()

        compose.runOnIdle {
            assertEquals("cme-1", openedCme?.id)
        }
    }

    @Test
    fun expandedSciencePaneUsesSamePairedEventCards() {
        compose.setContent { SolarActivityScreen(populatedEventState(), expanded = true) }

        compose.onNodeWithTag("solar-activity-expanded-scroll")
            .performScrollToNode(hasTestTag("recent-events-row"))

        compose.onNodeWithTag("recent-events-row").assertExists()
        compose.onNodeWithTag("recent-flares-card").assertExists()
        compose.onNodeWithTag("recent-cmes-card").assertExists()
        compose.onNodeWithTag("solar-activity-expanded-imagery").assertExists()
    }

    @Test
    fun hmiAndEnlilOpenTrueFullscreenAndSelectionSurvivesClose() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        assertTrue(compose.onAllNodesWithText("4532").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("4533").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("solar-imagery-stage-hmi").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertExists()
        compose.onNodeWithTag("fullscreen-active-region-overlay").assertExists()
        assertTrue(compose.onAllNodesWithText("4532").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("4533").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("fullscreen-imagery-close").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertDoesNotExist()
        compose.onNodeWithTag("solar-imagery-stage-hmi").assertExists()

        compose.onNodeWithTag("solar-imagery-enlil").performClick()
        compose.onNodeWithTag("solar-imagery-stage-enlil").performClick()
        compose.onNodeWithTag("fullscreen-enlil-player").assertExists()
        compose.onNodeWithTag("fullscreen-imagery-close").performClick()
        compose.onNodeWithTag("solar-imagery-stage-enlil").assertExists()
    }
    @Test
    fun c2RepositoryLoadingShowsMediaSpinner() {
        compose.setContent {
            SolarActivityScreen(
                populatedImageryState().copy(lascoC2 = RepositoryState.Loading),
                expanded = false,
            )
        }

        compose.onNodeWithTag("solar-imagery-c2").performClick()
        compose.onNodeWithTag("solar-media-loading").assertExists()
    }

    @Test
    fun enlilRepositoryLoadingShowsSpinnerAndEmptyShowsUnavailable() {
        compose.setContent {
            SolarActivityScreen(
                populatedImageryState().copy(enlil = RepositoryState.Loading),
                expanded = false,
            )
        }

        compose.onNodeWithTag("solar-imagery-enlil").performClick()
        compose.onNodeWithTag("enlil-loading").assertExists()

        compose.setContent {
            SolarActivityScreen(
                populatedImageryState().copy(
                    enlil = RepositoryState.Empty(source, DataFreshness.Fresh),
                ),
                expanded = false,
            )
        }

        compose.onNodeWithTag("solar-imagery-enlil").performClick()
        compose.onNodeWithTag("enlil-unavailable").assertExists()
    }

    @Test
    fun c2AndC3OpenFullscreenViewer() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        compose.onNodeWithTag("solar-imagery-c2").performClick()
        compose.onNodeWithTag("solar-imagery-stage-c2").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertExists()
        compose.onNodeWithTag("fullscreen-solar-media-loading").assertExists()
        compose.onNodeWithTag("fullscreen-imagery-close").performClick()

        compose.onNodeWithTag("solar-imagery-c3").performClick()
        compose.onNodeWithTag("solar-imagery-stage-c3").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertExists()
        compose.onNodeWithTag("fullscreen-solar-media-loading").assertExists()
    }

}
