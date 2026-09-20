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
        val engineArgument = source.indexOf("engine = engine", startIndex = sceneViewStart)
        val sceneViewModifierArguments = source.substring(sceneViewStart, engineArgument)

        assertTrue(
            "SceneView must receive pointerInput on its own modifier chain so AndroidView interop can yield/claim gestures correctly",
            sceneViewModifierArguments.contains(".pointerInput(touchSlop)"),
        )
    }
}
