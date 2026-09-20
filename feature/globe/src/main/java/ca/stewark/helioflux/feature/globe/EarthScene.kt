package ca.stewark.helioflux.feature.globe

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.stewark.helioflux.core.model.AuroraPoint
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.texture.ImageTexture
import kotlin.math.cos
import kotlin.math.sin

internal const val EARTH_TEXTURE_ASSET = "textures/earth_night.jpg"
internal const val EARTH_RADIUS = 1.0f

@Composable
fun EarthScene(
    points: List<AuroraPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    var interaction by remember { mutableStateOf(GlobeInteractionState()) }

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

    LaunchedEffect(Unit) {
        var previousNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (previousNanos != 0L) {
                    interaction = interaction.advance(
                        deltaMillis = (nanos - previousNanos) / 1_000_000L,
                        nowMillis = SystemClock.uptimeMillis(),
                    )
                }
                previousNanos = nanos
            }
        }
    }

    SideEffect {
        val yaw = Math.toRadians(interaction.yawDegrees.toDouble())
        val pitch = Math.toRadians(interaction.pitchDegrees.toDouble())
        val horizontal = interaction.cameraDistance * cos(pitch).toFloat()
        cameraNode.position = Position(
            x = horizontal * sin(yaw).toFloat(),
            y = interaction.cameraDistance * sin(pitch).toFloat(),
            z = horizontal * cos(yaw).toFloat(),
        )
        cameraNode.lookAt(Position(x = 0f, y = 0f, z = 0f))
    }

    val gestures = rememberOnGestureListener(
        onDown = { _, _ ->
            interaction = interaction.beginInteraction(SystemClock.uptimeMillis())
        },
        onScroll = { _, _, _, distance ->
            interaction = interaction.drag(
                deltaX = distance.x,
                deltaY = distance.y,
                nowMillis = SystemClock.uptimeMillis(),
            )
        },
        onMoveEnd = { _, _, _ ->
            interaction = interaction.endInteraction(SystemClock.uptimeMillis())
        },
        onScaleBegin = { _, _, _ ->
            interaction = interaction.beginInteraction(SystemClock.uptimeMillis())
        },
        onScale = { detector, _, _ ->
            interaction = interaction.scale(detector.scaleFactor, SystemClock.uptimeMillis())
        },
        onScaleEnd = { _, _, _ ->
            interaction = interaction.endInteraction(SystemClock.uptimeMillis())
        },
    )

    SceneView(
        modifier = modifier,
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = null,
        onGestureListener = gestures,
        autoCenterContent = false,
        mainLightNode = null,
        fillLightNode = null,
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
