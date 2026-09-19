package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.model.AuroraPoint
import org.junit.Rule
import org.junit.Test

class EarthSceneLifecycleTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun earthAuroraAndAtmosphereSurviveCompositionRecreation() {
        var mounted by mutableStateOf(true)
        val points = listOf(AuroraPoint(65.0, -50.0, 26.0))

        composeRule.setContent {
            if (mounted) AuroraGlobe(points = points)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { mounted = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { mounted = true }
        composeRule.waitForIdle()
    }
}
