package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.ActiveRegion
import ca.stewark.helioflux.core.model.SolarImage
import kotlin.math.roundToInt

internal val ActiveRegionLabelMinGap = 10.dp
internal val ActiveRegionAnchorGap = 6.dp
internal val ActiveRegionMaxHorizontalNudge = 24.dp
internal val ActiveRegionHorizontalNudgeStep = 8.dp

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
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = LocalTextStyle.current
    val minGapPx = with(density) { ActiveRegionLabelMinGap.toPx() }
    val anchorGapPx = with(density) { ActiveRegionAnchorGap.toPx() }
    val maxHorizontalNudgePx = with(density) { ActiveRegionMaxHorizontalNudge.toPx() }
    val horizontalNudgeStepPx = with(density) { ActiveRegionHorizontalNudgeStep.toPx() }
    val labelByKey = regions.associate { it.id to (it.number ?: it.id) }

    val placements =
        if (width > 0 && height > 0) {
            resolveActiveRegionLabels(
                labels =
                    regions.map { region ->
                        val p = mapActiveRegion(region.helioprojectiveX, region.helioprojectiveY)
                        val label = region.number ?: region.id
                        ActiveRegionLabelInput(
                            key = region.id,
                            anchor =
                                Offset(
                                    (p.x * width).toFloat(),
                                    (p.y * height).toFloat(),
                                ),
                            size = textMeasurer.measure(label, style = labelStyle).size,
                        )
                    },
                stageSize = IntSize(width, height),
                minLabelGapPx = minGapPx,
                anchorGapPx = anchorGapPx,
                maxHorizontalNudgePx = maxHorizontalNudgePx,
                horizontalNudgeStepPx = horizontalNudgeStepPx,
            )
        } else {
            emptyList()
        }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged {
                width = it.width
                height = it.height
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            placements.forEach { placement ->
                placement.leaderEnd?.let { leaderEnd ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = placement.anchor,
                        end = leaderEnd,
                        strokeWidth = 1.25.dp.toPx(),
                    )
                }
            }
        }
        placements.forEach { placement ->
            Text(
                text = labelByKey.getValue(placement.key),
                modifier =
                    Modifier.offset {
                        IntOffset(
                            placement.topLeft.x.roundToInt(),
                            placement.topLeft.y.roundToInt(),
                        )
                    },
                style = labelStyle,
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
