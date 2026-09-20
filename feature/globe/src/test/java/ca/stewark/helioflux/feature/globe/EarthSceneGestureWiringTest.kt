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
}
