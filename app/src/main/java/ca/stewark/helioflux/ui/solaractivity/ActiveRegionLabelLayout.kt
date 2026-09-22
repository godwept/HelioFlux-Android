package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class ActiveRegionLabelInput(
    val key: String,
    val anchor: Offset,
    val size: IntSize,
)

internal data class ActiveRegionLabelPlacement(
    val key: String,
    val anchor: Offset,
    val topLeft: Offset,
    val size: IntSize,
    val leaderEnd: Offset?,
)

private data class LabelBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private data class LabelCandidate(
    val topLeft: Offset,
    val displacement: Float,
    val order: Int,
)

internal fun resolveActiveRegionLabels(
    labels: List<ActiveRegionLabelInput>,
    stageSize: IntSize,
    minGapPx: Float,
    maxDisplacementPx: Float,
    leaderThresholdPx: Float,
): List<ActiveRegionLabelPlacement> {
    if (stageSize.width <= 0 || stageSize.height <= 0) return emptyList()

    val placed = mutableListOf<ActiveRegionLabelPlacement>()
    labels.sortedBy(ActiveRegionLabelInput::key).forEach { label ->
        val candidates =
            candidatesFor(
                label = label,
                stageSize = stageSize,
                minGapPx = minGapPx,
                maxDisplacementPx = maxDisplacementPx,
            )
        val selected =
            candidates.firstOrNull { candidate ->
                val bounds = bounds(candidate.topLeft, label.size)
                placed.none { existing ->
                    conflicts(bounds, bounds(existing.topLeft, existing.size), minGapPx)
                }
            } ?: candidates.minWithOrNull(
                compareBy<LabelCandidate> { candidate ->
                    val candidateBounds = bounds(candidate.topLeft, label.size)
                    placed.sumOf { existing ->
                        overlapArea(
                            candidateBounds,
                            bounds(existing.topLeft, existing.size),
                            minGapPx,
                        ).toDouble()
                    }
                }.thenBy(LabelCandidate::displacement)
                    .thenBy(LabelCandidate::order),
            ) ?: fallbackCandidate(label, stageSize)

        val center = center(selected.topLeft, label.size)
        val displacement = distance(label.anchor, center)
        val labelBounds = bounds(selected.topLeft, label.size)
        val leaderEnd =
            if (displacement > leaderThresholdPx) {
                edgeTowardAnchor(label.anchor, labelBounds)
            } else {
                null
            }

        placed +=
            ActiveRegionLabelPlacement(
                key = label.key,
                anchor = label.anchor,
                topLeft = selected.topLeft,
                size = label.size,
                leaderEnd = leaderEnd,
            )
    }
    return placed
}

private fun candidatesFor(
    label: ActiveRegionLabelInput,
    stageSize: IntSize,
    minGapPx: Float,
    maxDisplacementPx: Float,
): List<LabelCandidate> {
    val result = mutableListOf<LabelCandidate>()
    var order = 0

    fun add(center: Offset, displacement: Float) {
        val topLeft =
            Offset(
                center.x - label.size.width / 2f,
                center.y - label.size.height / 2f,
            )
        if (isInBounds(topLeft, label.size, stageSize)) {
            result += LabelCandidate(topLeft, displacement, order)
        }
        order++
    }

    add(label.anchor, 0f)

    val maxDistance = maxDisplacementPx.coerceAtLeast(0f)
    if (maxDistance == 0f) return result

    val step = minGapPx.coerceAtLeast(1f)
    val rings = mutableListOf<Float>()
    var radius = step
    while (radius < maxDistance) {
        rings += radius
        radius += step
    }
    if (rings.isEmpty() || abs(rings.last() - maxDistance) > 0.001f) {
        rings += maxDistance
    }

    val diagonalScale = (1.0 / sqrt(2.0)).toFloat()
    rings.forEach { r ->
        val diagonal = r * diagonalScale
        val offsets =
            listOf(
                Offset(0f, -r),
                Offset(0f, r),
                Offset(-r, 0f),
                Offset(r, 0f),
                Offset(-diagonal, -diagonal),
                Offset(diagonal, -diagonal),
                Offset(-diagonal, diagonal),
                Offset(diagonal, diagonal),
            )
        offsets.forEach { offset ->
            add(label.anchor + offset, r)
        }
    }
    return result
}

private fun fallbackCandidate(
    label: ActiveRegionLabelInput,
    stageSize: IntSize,
): LabelCandidate {
    val maxX = max(0f, stageSize.width - label.size.width.toFloat())
    val maxY = max(0f, stageSize.height - label.size.height.toFloat())
    val centered =
        Offset(
            label.anchor.x - label.size.width / 2f,
            label.anchor.y - label.size.height / 2f,
        )
    val topLeft =
        Offset(
            centered.x.coerceIn(0f, maxX),
            centered.y.coerceIn(0f, maxY),
        )
    return LabelCandidate(
        topLeft = topLeft,
        displacement = distance(label.anchor, center(topLeft, label.size)),
        order = Int.MAX_VALUE,
    )
}

private fun bounds(
    topLeft: Offset,
    size: IntSize,
) = LabelBounds(
    left = topLeft.x,
    top = topLeft.y,
    right = topLeft.x + size.width,
    bottom = topLeft.y + size.height,
)

private fun center(
    topLeft: Offset,
    size: IntSize,
) = Offset(
    topLeft.x + size.width / 2f,
    topLeft.y + size.height / 2f,
)

private fun isInBounds(
    topLeft: Offset,
    size: IntSize,
    stageSize: IntSize,
): Boolean =
    topLeft.x >= 0f &&
        topLeft.y >= 0f &&
        topLeft.x + size.width <= stageSize.width &&
        topLeft.y + size.height <= stageSize.height

private fun conflicts(
    a: LabelBounds,
    b: LabelBounds,
    gap: Float,
): Boolean =
    !(
        a.right + gap <= b.left ||
            b.right + gap <= a.left ||
            a.bottom + gap <= b.top ||
            b.bottom + gap <= a.top
    )

private fun overlapArea(
    a: LabelBounds,
    b: LabelBounds,
    gap: Float,
): Float {
    val padding = gap / 2f
    val left = max(a.left - padding, b.left - padding)
    val top = max(a.top - padding, b.top - padding)
    val right = min(a.right + padding, b.right + padding)
    val bottom = min(a.bottom + padding, b.bottom + padding)
    return max(0f, right - left) * max(0f, bottom - top)
}

private fun edgeTowardAnchor(
    anchor: Offset,
    bounds: LabelBounds,
): Offset {
    val center =
        Offset(
            (bounds.left + bounds.right) / 2f,
            (bounds.top + bounds.bottom) / 2f,
        )
    val dx = anchor.x - center.x
    val dy = anchor.y - center.y
    if (abs(dx) < 0.001f && abs(dy) < 0.001f) return center

    val halfWidth = (bounds.right - bounds.left) / 2f
    val halfHeight = (bounds.bottom - bounds.top) / 2f
    val scaleX = if (abs(dx) < 0.001f) Float.POSITIVE_INFINITY else halfWidth / abs(dx)
    val scaleY = if (abs(dy) < 0.001f) Float.POSITIVE_INFINITY else halfHeight / abs(dy)
    val scale = min(scaleX, scaleY)

    return Offset(
        center.x + dx * scale,
        center.y + dy * scale,
    )
}

private fun distance(
    a: Offset,
    b: Offset,
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
}
