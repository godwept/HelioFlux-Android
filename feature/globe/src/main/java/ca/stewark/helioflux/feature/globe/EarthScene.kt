package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.stewark.helioflux.core.model.AuroraPoint
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.ImageTexture

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

    val earthTexture = remember(engine) {
        ImageTexture.Builder()
            .bitmap(context.assets, EARTH_TEXTURE_ASSET)
            .build(engine)
    }
    DisposableEffect(engine, earthTexture) {
        onDispose { engine.safeDestroyTexture(earthTexture) }
    }
    val earthMaterial = remember(materialLoader, earthTexture) {
        materialLoader.createTextureInstance(
            texture = earthTexture,
            metallic = 0f,
            roughness = 0.9f,
            reflectance = 0.1f,
        )
    }

    DisposableEffect(cameraNode) {
        cameraNode.position = Position(x = -2.15f, y = 2.65f, z = 2.55f)
        cameraNode.lookAt(Position(x = 0f, y = 0.35f, z = 0f))
        onDispose { }
    }

    SceneView(
        modifier = modifier,
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = null,
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
