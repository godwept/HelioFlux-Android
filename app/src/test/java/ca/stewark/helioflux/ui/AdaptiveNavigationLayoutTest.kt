package ca.stewark.helioflux.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationLayoutTest {
    @Test
    fun navigationRailItemsAreCenteredInAvailableLandscapeHeight() {
        assertEquals(292.dp, navigationRailGroupTop(800.dp, 3))
    }

    @Test
    fun navigationRailIndicatorTracksTheCenteredSelectedItem() {
        assertEquals(316.dp, navigationRailIndicatorOffset(800.dp, 0, 3))
        assertEquals(460.dp, navigationRailIndicatorOffset(800.dp, 2, 3))
    }

    @Test
    fun navigationRailDoesNotOffsetAboveViewportWhenHeightIsTight() {
        assertEquals(0.dp, navigationRailGroupTop(180.dp, 3))
    }
}
