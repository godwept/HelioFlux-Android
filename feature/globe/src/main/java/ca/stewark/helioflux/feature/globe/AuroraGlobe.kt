package ca.stewark.helioflux.feature.globe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.helioflux.core.model.AuroraPoint

@Composable
fun AuroraGlobe(
    modifier: Modifier = Modifier,
    points: List<AuroraPoint> = emptyList(),
) {
    EarthScene(points = points, modifier = modifier)
}
