package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import org.junit.Rule
import org.junit.Test

class SolarActivityUiTest {
    @get:Rule val compose = createComposeRule()
    private val source = DataSourceKey("test")

    private fun populatedImageryState(): SolarActivityUiState =
        SolarActivityUiState(
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
                    listOf(ActiveRegion("r1", "12345", 0.0, 0.0)),
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
    fun hmiAndEnlilOpenTrueFullscreenAndSelectionSurvivesClose() {
        compose.setContent { SolarActivityScreen(populatedImageryState(), expanded = false) }

        compose.onNodeWithTag("solar-imagery-stage-hmi").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertExists()
        compose.onNodeWithTag("fullscreen-active-region-overlay").assertExists()
        compose.onNodeWithTag("fullscreen-imagery-close").performClick()
        compose.onNodeWithTag("fullscreen-imagery-viewer").assertDoesNotExist()
        compose.onNodeWithTag("solar-imagery-stage-hmi").assertExists()

        compose.onNodeWithTag("solar-imagery-enlil").performClick()
        compose.onNodeWithTag("solar-imagery-stage-enlil").performClick()
        compose.onNodeWithTag("fullscreen-enlil-player").assertExists()
        compose.onNodeWithTag("fullscreen-imagery-close").performClick()
        compose.onNodeWithTag("solar-imagery-stage-enlil").assertExists()
    }
}
