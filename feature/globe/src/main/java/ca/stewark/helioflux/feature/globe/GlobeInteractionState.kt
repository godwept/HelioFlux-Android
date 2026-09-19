package ca.stewark.helioflux.feature.globe

import kotlin.math.max
import kotlin.math.min

data class GlobeInteractionState(
    val yawDegrees: Float = -40f,
    val pitchDegrees: Float = 35f,
    val cameraDistance: Float = 4.25f,
    val isInteracting: Boolean = false,
    val lastInteractionMillis: Long = Long.MIN_VALUE,
) {
    fun advance(deltaMillis: Long, nowMillis: Long): GlobeInteractionState {
        if (isInteracting || (lastInteractionMillis != Long.MIN_VALUE && nowMillis - lastInteractionMillis < AUTO_ROTATE_RESUME_MILLIS)) return this
        return copy(yawDegrees = wrapDegrees(yawDegrees + deltaMillis * AUTO_ROTATE_DEGREES_PER_MILLISECOND))
    }

    fun beginInteraction(nowMillis: Long) = copy(
        isInteracting = true,
        lastInteractionMillis = nowMillis,
    )

    fun drag(deltaX: Float, deltaY: Float, nowMillis: Long) = copy(
        yawDegrees = wrapDegrees(yawDegrees - deltaX * DRAG_DEGREES_PER_PIXEL),
        pitchDegrees = (pitchDegrees + deltaY * DRAG_DEGREES_PER_PIXEL).coerceIn(MIN_PITCH, MAX_PITCH),
        isInteracting = true,
        lastInteractionMillis = nowMillis,
    )

    fun scale(scaleFactor: Float, nowMillis: Long): GlobeInteractionState {
        if (!scaleFactor.isFinite() || scaleFactor <= 0f) return this
        return copy(
            cameraDistance = (cameraDistance / scaleFactor).coerceIn(MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE),
            isInteracting = true,
            lastInteractionMillis = nowMillis,
        )
    }

    fun endInteraction(nowMillis: Long) = copy(
        isInteracting = false,
        lastInteractionMillis = nowMillis,
    )

    companion object {
        const val MIN_CAMERA_DISTANCE = 2.2f
        const val MAX_CAMERA_DISTANCE = 5.5f
        const val AUTO_ROTATE_RESUME_MILLIS = 3_000L
        const val AUTO_ROTATE_DEGREES_PER_MILLISECOND = 0.0035f
        const val DRAG_DEGREES_PER_PIXEL = 0.12f
        const val MIN_PITCH = -70f
        const val MAX_PITCH = 70f

        private fun wrapDegrees(value: Float): Float {
            val wrapped = value % 360f
            return if (wrapped < 0f) wrapped + 360f else wrapped
        }
    }
}
