package ca.stewark.helioflux.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class SectionStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statesRenderIndependently() {
        assertState(SectionState.Loading, "section-loading")
        assertState(SectionState.Error("Network unavailable"), "section-error")
        assertState(SectionState.Empty(), "section-empty")
        assertState(SectionState.Content, "section-content")
    }

    private fun assertState(state: SectionState, expectedTag: String) {
        composeRule.setContent {
            SectionStateContent(state = state) {
                Text("content", modifier = Modifier.testTag("section-content"))
            }
        }
        composeRule.onNodeWithTag(expectedTag).assertIsDisplayed()
    }
}
