package ca.stewark.helioflux.feature.globe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuroraGlobeRenderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mountsAndUnmountsEmptySceneWithoutCrashing() {
        composeRule.setContent { AuroraGlobe() }
        composeRule.waitForIdle()
        composeRule.setContent { }
        composeRule.waitForIdle()
    }
}
