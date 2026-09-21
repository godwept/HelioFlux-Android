package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.ActiveRegion
import ca.stewark.helioflux.core.model.SolarImage
import kotlin.math.roundToInt

data class NormalizedRegionPosition(
    val x: Double,
    val y: Double,
)

fun mapActiveRegion(
    xArcsec: Double,
    yArcsec: Double,
) = NormalizedRegionPosition(
    ((512.0 + xArcsec / 1000.0 * 512.0) / 1024.0).coerceIn(0.0, 1.0),
    ((512.0 - yArcsec / 1000.0 * 512.0) / 1024.0).coerceIn(0.0, 1.0),
)

@Composable
internal fun BoxScope.ActiveRegionOverlay(
    regions: List<ActiveRegion>,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged {
                width = it.width
                height = it.height
            },
    ) {
        regions.forEach { region ->
            val p = mapActiveRegion(region.helioprojectiveX, region.helioprojectiveY)
            Text(
                region.number ?: region.id,
                Modifier.offset {
                    IntOffset(
                        (p.x * width).roundToInt(),
                        (p.y * height).roundToInt(),
                    )
                },
            )
        }
    }
}

@Composable
fun MagnetogramCard(
    state: RepositoryState<SolarImage>,
    regions: List<ActiveRegion>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolarImageryCard(
        title = "HMI Magnetogram",
        source = "SDO / HMI",
        state = state,
        onClick = onClick,
        modifier = modifier,
        overlay = { ActiveRegionOverlay(regions) },
    )
}
