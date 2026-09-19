package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import ca.stewark.helioflux.core.model.AuroraPoint
import io.github.sceneview.SceneScope
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.ImageTexture

internal const val AURORA_RADIUS = 1.012f

@Composable
internal fun SceneScope.AuroraLayer(
    points: List<AuroraPoint>,
    materialLoader: MaterialLoader,
) {
    if (points.none { it.intensity >= 5.0 }) return

    val overlay = remember(points) { AuroraTextureGenerator.generate(points) }
    val texture = remember(engine, overlay) {
        ImageTexture.Builder().bitmap(overlay).build(engine)
    }
    DisposableEffect(engine, texture, overlay) {
        onDispose {
            engine.safeDestroyTexture(texture)
            overlay.recycle()
        }
    }
    val material = remember(materialLoader, texture) {
        materialLoader.createTextureInstance(
            texture = texture,
            isOpaque = false,
            metallic = 0f,
            roughness = 1f,
            reflectance = 0f,
        )
    }

    SphereNode(
        radius = AURORA_RADIUS,
        stacks = 48,
        slices = 96,
        materialInstance = material,
    )
}
