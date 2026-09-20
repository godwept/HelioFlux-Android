package ca.stewark.helioflux.feature.globe

import kotlin.math.abs
import kotlin.math.hypot

internal enum class GlobeGestureIntent {
    Undecided,
    GlobeDrag,
    ParentScroll,
    GlobeTransform,
}

internal fun globeGestureIntent(
    current: GlobeGestureIntent,
    totalDeltaX: Float,
    totalDeltaY: Float,
    pointerCount: Int,
    touchSlop: Float,
): GlobeGestureIntent {
    if (current != GlobeGestureIntent.Undecided) return current
    if (pointerCount >= 2) return GlobeGestureIntent.GlobeTransform
    if (hypot(totalDeltaX, totalDeltaY) < touchSlop) return GlobeGestureIntent.Undecided

    return if (abs(totalDeltaX) > abs(totalDeltaY)) {
        GlobeGestureIntent.GlobeDrag
    } else {
        GlobeGestureIntent.ParentScroll
    }
}
