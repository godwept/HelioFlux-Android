package ca.stewark.helioflux.feature.globe

import android.graphics.Bitmap
import android.graphics.Color
import ca.stewark.helioflux.core.model.AuroraPoint
import kotlin.math.max
import kotlin.math.roundToInt

object AuroraTextureGenerator {
    fun generate(points: List<AuroraPoint>, width: Int = 1024, height: Int = 512): Bitmap {
        require(width > 0 && height > 0)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val radius = max(1, (width * 0.7 / 360.0).roundToInt())
        for (point in points) {
            val rgba = AuroraPalette.colorFor(point.intensity)
            if (rgba.isTransparent) continue
            val center = AuroraTextureMapper.map(point.latitude, point.longitude, width, height)
            val color = Color.argb(rgba.alpha, rgba.red, rgba.green, rgba.blue)
            for (dy in -radius..radius) for (dx in -radius..radius) {
                if (dx * dx + dy * dy > radius * radius) continue
                val x = ((center.x + dx) % width + width) % width
                val y = (center.y + dy).coerceIn(0, height - 1)
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
