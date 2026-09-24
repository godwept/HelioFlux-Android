package ca.stewark.helioflux.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGestureRegionsTest {
    @Test fun foregroundBoundsProtectTheirSurfaceButLeaveGapsExposed() {
        val regions = HomeGestureRegions()
        regions.update("title", Rect(100f, 0f, 300f, 50f))
        regions.update("metric", Rect(0f, 70f, 100f, 110f))
        regions.update("heading", Rect(0f, 130f, 180f, 160f))
        regions.update("card", Rect(0f, 170f, 280f, 370f))

        assertFalse(regions.isExposed(Offset(150f, 25f)))
        assertFalse(regions.isExposed(Offset(50f, 90f)))
        assertFalse(regions.isExposed(Offset(50f, 145f)))
        assertFalse(regions.isExposed(Offset(50f, 200f)))
        assertTrue(regions.isExposed(Offset(330f, 200f)))
        assertTrue(regions.isExposed(Offset(50f, 55f)))
    }

    @Test fun disposedLazyCardNoLongerProtectsOldPosition() {
        val regions = HomeGestureRegions()
        regions.update("card", Rect(0f, 170f, 280f, 370f))
        regions.remove("card")
        assertTrue(regions.isExposed(Offset(50f, 200f)))
    }
}
