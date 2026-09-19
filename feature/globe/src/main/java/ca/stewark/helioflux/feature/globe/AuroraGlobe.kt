package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.stewark.helioflux.core.model.AuroraPoint

@Composable
fun AuroraGlobe(
    modifier: Modifier = Modifier,
    points: List<AuroraPoint> = emptyList(),
    onRendererUnavailable: () -> Unit = {},
) {
    val context = LocalContext.current
    val textureAvailable = remember(context) {
        runCatching {
            context.assets.open(EARTH_TEXTURE_ASSET).use { it.read() >= 0 }
        }.getOrDefault(false)
    }

    if (!textureAvailable) {
        LaunchedEffect(Unit) { onRendererUnavailable() }
        return
    }

    EarthScene(points = points, modifier = modifier)
}
