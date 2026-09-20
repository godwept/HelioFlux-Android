package ca.stewark.helioflux.feature.globe

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.zIndex
import ca.stewark.helioflux.core.model.AuroraPoint
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.texture.ImageTexture
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val EARTH_TEXTURE_ASSET = "textures/earth_night.jpg"
internal const val EARTH_RADIUS = 1.0f

@Composable
fun EarthScene(
    points: List<AuroraPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
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

    Box(modifier = modifier) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            onGestureListener = null,
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

        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(1f)
                .pointerInput(touchSlop) {
                    awaitPointerEventScope {
                        while (true) {
                            var event = awaitPointerEvent()
                            while (event.changes.none { it.pressed }) {
                                event = awaitPointerEvent()
                            }

                            var intent = GlobeGestureIntent.Undecided
                            var totalDelta = Offset.Zero
                            var interactionActive = false
                            var previousSpan: Float? = null

                            while (true) {
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    if (interactionActive) {
                                        interaction = interaction.endInteraction(SystemClock.uptimeMillis())
                                    }
                                    break
                                }

                                val nowMillis = SystemClock.uptimeMillis()
                                if (intent == GlobeGestureIntent.Undecided) {
                                    if (pressed.size == 1) {
                                        val change = pressed.first()
                                        totalDelta += change.position - change.previousPosition
                                    }

                                    intent = globeGestureIntent(
                                        current = intent,
                                        totalDeltaX = totalDelta.x,
                                        totalDeltaY = totalDelta.y,
                                        pointerCount = pressed.size,
                                        touchSlop = touchSlop,
                                    )

                                    when (intent) {
                                        GlobeGestureIntent.GlobeDrag -> {
                                            interaction = interaction
                                                .beginInteraction(nowMillis)
                                                .drag(
                                                    deltaX = -totalDelta.x,
                                                    deltaY = -totalDelta.y,
                                                    nowMillis = nowMillis,
                                                )
                                            interactionActive = true
                                            pressed.forEach { it.consume() }
                                        }

                                        GlobeGestureIntent.GlobeTransform -> {
                                            interaction = interaction.beginInteraction(nowMillis)
                                            interactionActive = true
                                            previousSpan = pointerSpan(pressed)
                                            pressed.forEach { it.consume() }
                                        }

                                        GlobeGestureIntent.Undecided,
                                        GlobeGestureIntent.ParentScroll,
                                        -> Unit
                                    }
                                } else {
                                    when (intent) {
                                        GlobeGestureIntent.GlobeDrag -> {
                                            val change = pressed.first()
                                            val delta = change.position - change.previousPosition
                                            interaction = interaction.drag(
                                                deltaX = -delta.x,
                                                deltaY = -delta.y,
                                                nowMillis = nowMillis,
                                            )
                                            pressed.forEach { it.consume() }
                                        }

                                        GlobeGestureIntent.GlobeTransform -> {
                                            val currentSpan = pointerSpan(pressed)
                                            val priorSpan = previousSpan
                                            if (currentSpan != null && priorSpan != null && priorSpan > 0f) {
                                                interaction = interaction.scale(
                                                    scaleFactor = currentSpan / priorSpan,
                                                    nowMillis = nowMillis,
                                                )
                                            }
                                            previousSpan = currentSpan ?: previousSpan
                                            pressed.forEach { it.consume() }
                                        }

                                        GlobeGestureIntent.Undecided,
                                        GlobeGestureIntent.ParentScroll,
                                        -> Unit
                                    }
                                }

                                event = awaitPointerEvent()
                            }
                        }
                    }
                },
        )
    }
}

private fun pointerSpan(changes: List<PointerInputChange>): Float? {
    if (changes.size < 2) return null
    val delta = changes[0].position - changes[1].position
    return sqrt(delta.x * delta.x + delta.y * delta.y)
}
