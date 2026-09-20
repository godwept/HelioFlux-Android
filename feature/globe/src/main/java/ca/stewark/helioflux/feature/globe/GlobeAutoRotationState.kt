package ca.stewark.helioflux.feature.globe

internal const val GLOBE_AUTO_ROTATE_DEGREES_PER_SECOND = 3.5f
internal const val GLOBE_AUTO_ROTATE_RESUME_DELAY_MILLIS = 3_000L

internal class GlobeAutoRotationState {
    private var isTouching = false
    private var lastTouchEndMillis: Long? = null

    fun onTouchDown() {
        isTouching = true
    }

    fun onTouchEnd(nowMillis: Long) {
        isTouching = false
        lastTouchEndMillis = nowMillis
    }

    fun shouldRotate(nowMillis: Long): Boolean {
        if (isTouching) return false
        val touchEnd = lastTouchEndMillis ?: return true
        return nowMillis - touchEnd >= GLOBE_AUTO_ROTATE_RESUME_DELAY_MILLIS
    }
}

internal fun autoRotationDeltaDegrees(deltaNanos: Long): Float =
    deltaNanos / 1_000_000_000f * GLOBE_AUTO_ROTATE_DEGREES_PER_SECOND
