package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeAutoRotationStateTest {
    @Test
    fun idleGlobeRotatesImmediately() {
        assertTrue(GlobeAutoRotationState().shouldRotate(nowMillis = 1_000))
    }

    @Test
    fun touchPausesRotationUntilResumeDelayExpires() {
        val state = GlobeAutoRotationState()
        state.onTouchDown()
        assertFalse(state.shouldRotate(nowMillis = 10_000))

        state.onTouchEnd(nowMillis = 2_000)
        assertFalse(state.shouldRotate(nowMillis = 4_999))
        assertTrue(state.shouldRotate(nowMillis = 5_000))
    }

    @Test
    fun oneSecondFrameDeltaUsesSlowRotationRate() {
        assertEquals(
            3.5f,
            autoRotationDeltaDegrees(deltaNanos = 1_000_000_000L),
            0.0001f,
        )
    }
}
