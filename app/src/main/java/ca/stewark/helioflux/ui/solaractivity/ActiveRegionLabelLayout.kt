package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    val horizontalNudge: Float,
    val order: Int,
)

internal fun resolveActiveRegionLabels(
    labels: List<ActiveRegionLabelInput>,
    stageSize: IntSize,
    minLabelGapPx: Float,
    anchorGapPx: Float,
    maxHorizontalNudgePx: Float,
    horizontalNudgeStepPx: Float,
): List<ActiveRegionLabelPlacement> {
    if (stageSize.width <= 0 || stageSize.height <= 0) return emptyList()

    val placed = mutableListOf<ActiveRegionLabelPlacement>()
    labels.sortedBy(ActiveRegionLabelInput::key).forEach { label ->
        val candidates =
            candidatesFor(
                label = label,
                stageSize = stageSize,
                anchorGapPx = anchorGapPx,
                maxHorizontalNudgePx = maxHorizontalNudgePx,
                horizontalNudgeStepPx = horizontalNudgeStepPx,
            )

        val selected =
            candidates.firstOrNull { candidate ->
                val candidateBounds = bounds(candidate.topLeft, label.size)
                placed.none { existing ->
                    conflicts(
                        candidateBounds,
                        bounds(existing.topLeft, existing.size),
                        minLabelGapPx,
                    )
                }
            } ?: candidates.minWithOrNull(
                compareBy<LabelCandidate> { candidate ->
                    val candidateBounds = bounds(candidate.topLeft, label.size)
                    placed.sumOf { existing ->
                        overlapArea(
                            candidateBounds,
                            bounds(existing.topLeft, existing.size),
                            minLabelGapPx,
                        ).toDouble()
                    }
                }.thenBy { abs(it.horizontalNudge) }
                    .thenBy(LabelCandidate::order),
            ) ?: fallbackCandidate(label, stageSize, anchorGapPx, maxHorizontalNudgePx)

        val labelBounds = bounds(selected.topLeft, label.size)
        val centerX = selected.topLeft.x + label.size.width / 2f
        val leaderEnd =
            if (abs(centerX - label.anchor.x) > 0.5f) {
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
    anchorGapPx: Float,
    maxHorizontalNudgePx: Float,
    horizontalNudgeStepPx: Float,
): List<LabelCandidate> {
    val result = mutableListOf<LabelCandidate>()
    var order = 0

    fun add(horizontalNudge: Float, above: Boolean) {
        val centerX = label.anchor.x + horizontalNudge
        val top =
            if (above) {
                label.anchor.y - anchorGapPx - label.size.height
            } else {
                label.anchor.y + anchorGapPx
            }
        val topLeft = Offset(centerX - label.size.width / 2f, top)
        if (isInBounds(topLeft, label.size, stageSize)) {
            result += LabelCandidate(topLeft, horizontalNudge, order)
        }
        order++
    }

    add(horizontalNudge = 0f, above = true)
    add(horizontalNudge = 0f, above = false)

    val maxNudge = maxHorizontalNudgePx.coerceAtLeast(0f)
    val step = horizontalNudgeStepPx.coerceAtLeast(1f)
    var magnitude = step
    while (magnitude <= maxNudge + 0.001f) {
        val actual = min(magnitude, maxNudge)
        add(horizontalNudge = -actual, above = true)
        add(horizontalNudge = -actual, above = false)
        add(horizontalNudge = actual, above = true)
        add(horizontalNudge = actual, above = false)
        if (actual >= maxNudge) break
        magnitude += step
        if (magnitude > maxNudge && actual < maxNudge) magnitude = maxNudge
    }

    return result
}

private fun fallbackCandidate(
    label: ActiveRegionLabelInput,
    stageSize: IntSize,
    anchorGapPx: Float,
    maxHorizontalNudgePx: Float,
): LabelCandidate {
    val halfWidth = label.size.width / 2f
    val minCenterX = halfWidth
    val maxCenterX = stageSize.width - halfWidth
    val desiredCenterX =
        label.anchor.x
            .coerceIn(label.anchor.x - maxHorizontalNudgePx, label.anchor.x + maxHorizontalNudgePx)
            .coerceIn(minCenterX, maxCenterX)
    val aboveTop = label.anchor.y - anchorGapPx - label.size.height
    val belowTop = label.anchor.y + anchorGapPx
    val top =
        when {
            aboveTop >= 0f -> aboveTop
            belowTop + label.size.height <= stageSize.height -> belowTop
            else -> belowTop.coerceIn(0f, max(0f, stageSize.height - label.size.height.toFloat()))
        }
    return LabelCandidate(
        topLeft = Offset(desiredCenterX - halfWidth, top),
        horizontalNudge = desiredCenterX - label.anchor.x,
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
