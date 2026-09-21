package ca.stewark.helioflux.ui.spaceweather

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.ui.components.ChartDomain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChartRefreshActionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lineChartRefreshActionInvokesCallback() {
        var calls = 0
        compose.setContent {
            SpaceWeatherLineCard(
                meta = densityChartMeta,
                series = emptyList(),
                domain = ChartDomain(0.0, 1.0),
                onRefresh = { calls++ },
                refreshContentDescription = "Refresh Density",
            )
        }

        compose.onNodeWithContentDescription("Refresh Density")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun busyRefreshActionIsDisabled() {
        compose.setContent {
            HemisphericPowerChart(
                series = emptyList(),
                onRefresh = {},
                refreshing = true,
            )
        }

        compose.onNodeWithContentDescription("Refresh Hemispheric Power").assertIsNotEnabled()
    }
}
