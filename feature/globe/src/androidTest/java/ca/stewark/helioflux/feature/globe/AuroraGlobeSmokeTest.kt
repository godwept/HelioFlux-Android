package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class AuroraGlobeSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mountsAndUnmountsEmptySceneViewWithoutCrashing() {
        var mounted by mutableStateOf(true)

        composeRule.setContent {
            if (mounted) {
                AuroraGlobe()
            }
        }

        composeRule.runOnIdle { mounted = false }
        composeRule.waitForIdle()
    }
}
