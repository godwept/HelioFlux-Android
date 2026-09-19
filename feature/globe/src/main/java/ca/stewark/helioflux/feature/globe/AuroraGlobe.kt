package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView

@Composable
fun AuroraGlobe(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> SceneView(context) },
        onRelease = { sceneView -> sceneView.destroy() },
    )
}
