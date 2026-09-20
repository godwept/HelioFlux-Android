package ca.stewark.helioflux.feature.globe

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ca.stewark.helioflux.core.model.AuroraPoint
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.texture.ImageTexture

internal const val EARTH_TEXTURE_ASSET = "textures/earth_night.jpg"
internal const val EARTH_RADIUS = 1.0f
private const val GLOBE_CAMERA_DISTANCE = 4.25f

@Composable
fun EarthScene(
    points: List<AuroraPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val cameraManipulator = rememberCameraManipulator(orbitRadius = GLOBE_CAMERA_DISTANCE)
    val autoRotation = remember { GlobeAutoRotationState() }

    val earthTexture = remember(engine) {
        ImageTexture.Builder().bitmap(context.assets, EARTH_TEXTURE_ASSET).build(engine)
    }
    val earthMaterial = remember(materialLoader, earthTexture) {
        materialLoader.createTextureInstance(
            texture = earthTexture,
            metallic = 0f,
            roughness = 0.9f,
            reflectance = 0.1f,
        )
    }

    SceneView(
        modifier = modifier,
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        autoCenterContent = false,
        mainLightNode = null,
        fillLightNode = null,
        onTouchEvent = { event, _ ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> autoRotation.onTouchDown()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> autoRotation.onTouchEnd(SystemClock.uptimeMillis())
            }
            // Returning false keeps SceneView's native orbit/pinch camera detector active.
            false
        },
    ) {
        Node(
            apply = {
                var previousFrameNanos = 0L
                onFrame = { frameTimeNanos ->
                    if (
                        previousFrameNanos != 0L &&
                        autoRotation.shouldRotate(SystemClock.uptimeMillis())
                    ) {
                        val deltaDegrees = autoRotationDeltaDegrees(
                            deltaNanos = frameTimeNanos - previousFrameNanos,
                        )
                        rotation = Rotation(
                            x = rotation.x,
                            y = (rotation.y + deltaDegrees) % 360f,
                            z = rotation.z,
                        )
                    }
                    previousFrameNanos = frameTimeNanos
                }
            },
        ) {
            SphereNode(
                radius = EARTH_RADIUS,
                stacks = 48,
                slices = 96,
                materialInstance = earthMaterial,
            )
            AuroraLayer(points = points, materialLoader = materialLoader)
            AtmosphereLayer(materialLoader = materialLoader)
        }
    }
}
