package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobeGestureIntentTest {
    @Test
    fun belowTouchSlopRemainsUndecided() {
        assertEquals(
            GlobeGestureIntent.Undecided,
            globeGestureIntent(
                current = GlobeGestureIntent.Undecided,
                totalDeltaX = 3f,
                totalDeltaY = 4f,
                pointerCount = 1,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun horizontalMovementClaimsGlobeDrag() {
        assertEquals(
            GlobeGestureIntent.GlobeDrag,
            globeGestureIntent(
                current = GlobeGestureIntent.Undecided,
                totalDeltaX = 12f,
                totalDeltaY = 4f,
                pointerCount = 1,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun verticalMovementYieldsToParentScroll() {
        assertEquals(
            GlobeGestureIntent.ParentScroll,
            globeGestureIntent(
                current = GlobeGestureIntent.Undecided,
                totalDeltaX = 4f,
                totalDeltaY = 12f,
                pointerCount = 1,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun multiTouchClaimsGlobeTransform() {
        assertEquals(
            GlobeGestureIntent.GlobeTransform,
            globeGestureIntent(
                current = GlobeGestureIntent.Undecided,
                totalDeltaX = 0f,
                totalDeltaY = 0f,
                pointerCount = 2,
                touchSlop = 8f,
            ),
        )
    }

    @Test
    fun claimedIntentDoesNotSwitchMidGesture() {
        assertEquals(
            GlobeGestureIntent.GlobeDrag,
            globeGestureIntent(
                current = GlobeGestureIntent.GlobeDrag,
                totalDeltaX = 1f,
                totalDeltaY = 100f,
                pointerCount = 1,
                touchSlop = 8f,
            ),
        )
    }
}
