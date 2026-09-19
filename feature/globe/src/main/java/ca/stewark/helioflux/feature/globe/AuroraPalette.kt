package ca.stewark.helioflux.feature.globe

data class AuroraRgba(val red: Int, val green: Int, val blue: Int, val alpha: Int) {
    val isTransparent: Boolean get() = alpha == 0
}

object AuroraPalette {
    fun colorFor(intensity: Double): AuroraRgba = when {
        intensity >= 41.0 -> AuroraRgba(255, 59, 48, alpha(0.72))
        intensity >= 26.0 -> AuroraRgba(255, 149, 0, alpha(0.62))
        intensity >= 16.0 -> AuroraRgba(52, 199, 89, alpha(0.50))
        intensity >= 9.0 -> AuroraRgba(0, 210, 190, alpha(0.36))
        intensity >= 5.0 -> AuroraRgba(90, 200, 250, alpha(0.22))
        else -> AuroraRgba(0, 0, 0, 0)
    }

    private fun alpha(opacity: Double) = (opacity * 255.0).toInt()
}
