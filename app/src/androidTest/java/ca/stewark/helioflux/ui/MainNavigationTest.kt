package ca.stewark.helioflux.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.window.core.layout.WindowSizeClass
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.CmeEvent
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.ui.home.HomeUiState
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import ca.stewark.helioflux.ui.solaractivity.SolarActivityUiState
import ca.stewark.helioflux.ui.spaceweather.SpaceWeatherUiState
import org.junit.Assert.assertEquals
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
    @Test
    fun cmeDetailsOpensLinkedPageFromApp() {
        val cme =
            CmeEvent(
                id = "cme-1",
                timestampMillis = 1789907640000L,
                speed = 1250.0,
                halfAngle = 35.0,
                direction = "NW",
                type = null,
                link = "https://example.test/cme-1",
            )
        val state =
            SolarActivityUiState(
                cmes =
                    RepositoryState.Available(
                        listOf(cme),
                        DataSourceKey("test"),
                        DataFreshness.Fresh,
                    ),
            )
        var openedUri: String? = null
        val uriHandler =
            object : UriHandler {
                override fun openUri(uri: String) {
                    openedUri = uri
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                HelioFluxApp(
                    windowSizeClass = WindowSizeClass.compute(400f, 800f),
                    solarActivityState = state,
                    initialDestination = HelioFluxDestination.SolarActivity,
                )
            }
        }

        rule.onNodeWithTag("solar-activity-compact")
            .performScrollToNode(hasText("Details"))
        rule.onNodeWithText("Details").performClick()

        rule.runOnIdle {
            assertEquals(cme.link, openedUri)
        }
    }

}
