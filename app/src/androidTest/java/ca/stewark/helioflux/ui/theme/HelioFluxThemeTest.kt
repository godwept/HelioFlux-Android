package ca.stewark.helioflux.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HelioFluxThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theme_usesDarkRootAndFixedSemanticBrandColors() {
        var background: Color? = null
        var primary: Color? = null
        var secondary: Color? = null

        composeRule.setContent {
            HelioFluxTheme {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    background = colors.background
                    primary = colors.primary
                    secondary = colors.secondary
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(SpaceBlack, background)
            assertEquals(SolarOrange, primary)
            assertEquals(DataCyan, secondary)
        }
    }
}
