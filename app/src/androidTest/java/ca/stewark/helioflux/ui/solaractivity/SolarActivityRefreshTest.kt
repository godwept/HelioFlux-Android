package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SolarActivityRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pullDownInvokesBroadRefresh() {
        var refreshes = 0
        compose.setContent {
            SolarActivityScreen(
                state = SolarActivityUiState(),
                expanded = false,
                onRefresh = { refreshes++ },
            )
        }

        compose.onNodeWithTag("solar-activity-pull-refresh").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, refreshes) }
    }
}
