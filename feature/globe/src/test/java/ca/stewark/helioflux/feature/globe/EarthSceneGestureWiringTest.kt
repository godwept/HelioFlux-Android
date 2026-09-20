package ca.stewark.helioflux.feature.globe

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class EarthSceneGestureWiringTest {
    @Test
    fun gestureInputIsAttachedToSceneViewInteropModifierChain() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/feature/globe/EarthScene.kt",
        ).readText()
        val sceneViewStart = source.indexOf("SceneView(")
        val sceneViewBodyStart = source.indexOf(") {", startIndex = sceneViewStart)
        val sceneViewCall = source.substring(sceneViewStart, sceneViewBodyStart)

        assertTrue(
            "SceneView must receive pointerInput on its own modifier chain so AndroidView interop can yield/claim gestures correctly",
            sceneViewCall.contains(".pointerInput(touchSlop)"),
        )
    }
}
