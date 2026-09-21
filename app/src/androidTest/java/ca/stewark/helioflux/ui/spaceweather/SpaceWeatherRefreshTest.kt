package ca.stewark.helioflux.ui.spaceweather

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SpaceWeatherRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun targetedChartActionRoutesAndPullRefreshRemainsBroad() {
        val targeted = mutableListOf<SpaceWeatherRefreshSource>()
        var broad = 0
        compose.setContent {
            SpaceWeatherScreen(
                state = SpaceWeatherUiState(),
                expanded = false,
                onTimeframe = {},
                onRefresh = { broad++ },
                onRefreshSource = { targeted += it },
            )
        }

        compose.onNodeWithContentDescription("Refresh IMF Bz / Bt").performClick()
        compose.onNodeWithTag("space-weather-pull-refresh").performTouchInput { swipeDown() }

        compose.runOnIdle {
            assertTrue(SpaceWeatherRefreshSource.Magnetic in targeted)
            assertEquals(1, broad)
        }
    }
}
