package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeInteractionStateTest {
    @Test fun autoRotationAdvancesWhileIdle() {
        val state = GlobeInteractionState(yawDegrees = 10f)
        val advanced = state.advance(deltaMillis = 1_000, nowMillis = 10_000)
        assertTrue(advanced.yawDegrees > 10f)
    }

    @Test fun interactionPausesAndIdleTimeoutResumesRotation() {
        val interacting = GlobeInteractionState(yawDegrees = 10f)
            .beginInteraction(1_000)
            .drag(20f, 0f, 1_100)
        assertEquals(interacting.yawDegrees, interacting.advance(1_000, 2_000).yawDegrees, 0f)

        val ended = interacting.endInteraction(2_000)
        assertFalse(ended.isInteracting)
        assertEquals(ended.yawDegrees, ended.advance(1_000, 4_999).yawDegrees, 0f)
        assertTrue(ended.advance(1_000, 5_000).yawDegrees != ended.yawDegrees)
    }

    @Test fun zoomAndPitchClampToSafeLimits() {
        val zoomedIn = GlobeInteractionState().scale(100f, 1_000)
        assertEquals(GlobeInteractionState.MIN_CAMERA_DISTANCE, zoomedIn.cameraDistance, 0f)
        val zoomedOut = zoomedIn.scale(0.001f, 2_000)
        assertEquals(GlobeInteractionState.MAX_CAMERA_DISTANCE, zoomedOut.cameraDistance, 0f)

        val pitched = GlobeInteractionState().drag(0f, 10_000f, 3_000)
        assertEquals(GlobeInteractionState.MAX_PITCH, pitched.pitchDegrees, 0f)
    }
}
