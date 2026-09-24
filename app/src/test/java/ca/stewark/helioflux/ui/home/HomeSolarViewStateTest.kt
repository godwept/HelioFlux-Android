package ca.stewark.helioflux.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSolarViewStateTest {
    @Test fun defaultViewIsSquareAndCentered() {
        val view = HomeSolarViewState()
        assertEquals(1f, view.scale)
        assertEquals(Offset.Zero, view.panPixels(IntSize(400, 800)))
        assertFalse(view.isBackground)
    }

    @Test fun zoomAndPanAreClampedAndViewportRelative() {
        val view = HomeSolarViewState().transform(2f, Offset(100f, -200f), IntSize(400, 800))
        assertEquals(2f, view.scale)
        assertEquals(Offset(100f, -200f), view.panPixels(IntSize(400, 800)))
        assertEquals(Offset(200f, -100f), view.panPixels(IntSize(800, 400)))
        assertEquals(4f, view.transform(10f, Offset.Zero, IntSize(400, 800)).scale)
        assertEquals(1f, view.transform(0.01f, Offset.Zero, IntSize(400, 800)).scale)
    }

    @Test fun invalidOrExcessiveSavedPositionIsConstrained() {
        val view = HomeSolarViewState(Float.NaN, Float.POSITIVE_INFINITY, 99f).normalized()
        assertEquals(HomeSolarViewState(), view)
        val constrained = HomeSolarViewState(2f, 10f, -10f).normalized()
        assertEquals(Offset(200f, -400f), constrained.panPixels(IntSize(400, 800)))
    }

    @Test fun resetAndNearOneSettleClearPosition() {
        val view = HomeSolarViewState(1.02f, 0.1f, 0.1f)
        assertEquals(HomeSolarViewState(), view.settle())
        assertEquals(HomeSolarViewState(), HomeSolarViewState(3f, 0.2f, 0.3f).reset())
        assertTrue(HomeSolarViewState(1.1f).settle().isBackground)
    }
}
