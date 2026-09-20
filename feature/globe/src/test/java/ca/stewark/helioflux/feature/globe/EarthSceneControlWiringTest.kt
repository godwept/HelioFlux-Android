package ca.stewark.helioflux.feature.globe

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarthSceneControlWiringTest {
    private val source = File(
        "src/main/java/ca/stewark/helioflux/feature/globe/EarthScene.kt",
    ).readText()

    @Test
    fun manualControlsUseSceneViewCameraManipulator() {
        assertTrue(source.contains("rememberCameraManipulator("))
        assertTrue(source.contains("cameraManipulator = cameraManipulator"))
        assertFalse(source.contains("cameraManipulator = null"))
    }

    @Test
    fun idleRotationRunsInsideSceneFrameInsteadOfComposePointerLoop() {
        assertTrue(source.contains("onFrame ="))
        assertTrue(source.contains("Node("))
        assertFalse(source.contains(".pointerInput("))
    }

    @Test
    fun touchOwnershipIsReportedForWholeAndroidGestureStream() {
        assertTrue(source.contains("onTouchActiveChanged: (Boolean) -> Unit"))
        assertTrue(source.contains("MotionEvent.ACTION_DOWN"))
        assertTrue(source.contains("onTouchActiveChanged(true)"))
        assertTrue(source.contains("MotionEvent.ACTION_CANCEL"))
        assertTrue(source.contains("onTouchActiveChanged(false)"))
    }
}
