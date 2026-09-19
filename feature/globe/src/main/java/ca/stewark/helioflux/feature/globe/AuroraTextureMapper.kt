package ca.stewark.helioflux.feature.globe

import kotlin.math.floor

data class TexturePixel(val x: Int, val y: Int)

object AuroraTextureMapper {
    fun map(latitude: Double, longitude: Double, width: Int, height: Int): TexturePixel {
        require(width > 0 && height > 0)
        val wrappedLongitude = ((longitude + 180.0) % 360.0 + 360.0) % 360.0
        val x = floor(wrappedLongitude / 360.0 * width).toInt().coerceIn(0, width - 1)
        val clampedLatitude = latitude.coerceIn(-90.0, 90.0)
        val y = floor((90.0 - clampedLatitude) / 180.0 * height).toInt().coerceIn(0, height - 1)
        return TexturePixel(x, y)
    }
}
