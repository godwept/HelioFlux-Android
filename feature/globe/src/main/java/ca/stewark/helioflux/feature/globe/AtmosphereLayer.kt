package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.sceneview.SceneScope
import io.github.sceneview.loaders.MaterialLoader

internal const val ATMOSPHERE_RADIUS = 1.04f

@Composable
internal fun SceneScope.AtmosphereLayer(materialLoader: MaterialLoader) {
    val material = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(Color(0x241A90FF))
    }
    SphereNode(
        radius = ATMOSPHERE_RADIUS,
        stacks = 36,
        slices = 72,
        materialInstance = material,
    )
}
