package ca.stewark.helioflux.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

internal const val SolarBackgroundEntryScale = 1.05f

data class HomeSolarViewState(
    val scale: Float = 1f,
    val panFractionX: Float = 0f,
    val panFractionY: Float = 0f,
) {
    val isBackground: Boolean get() = scale > 1f

    fun normalized(): HomeSolarViewState {
        if (!scale.isFinite() || !panFractionX.isFinite() || !panFractionY.isFinite()) {
            return HomeSolarViewState()
        }
        val safeScale = scale.coerceIn(1f, 4f)
        if (safeScale == 1f) return HomeSolarViewState()
        val limit = (safeScale - 1f) / 2f
        return HomeSolarViewState(
            safeScale,
            panFractionX.coerceIn(-limit, limit),
            panFractionY.coerceIn(-limit, limit),
        )
    }

    fun panPixels(viewport: IntSize): Offset =
        Offset(panFractionX * viewport.width, panFractionY * viewport.height)

    fun transform(zoom: Float, pan: Offset, viewport: IntSize): HomeSolarViewState {
        if (!zoom.isFinite() || !pan.x.isFinite() || !pan.y.isFinite()) return this
        val nextScale = (scale * zoom).coerceIn(1f, 4f)
        if (nextScale == 1f) return reset()
        return copy(
            scale = nextScale,
            panFractionX = panFractionX + pan.x / viewport.width.coerceAtLeast(1),
            panFractionY = panFractionY + pan.y / viewport.height.coerceAtLeast(1),
        ).normalized()
    }

    fun settle(): HomeSolarViewState =
        if (scale < SolarBackgroundEntryScale) reset() else normalized()

    fun reset(): HomeSolarViewState = HomeSolarViewState()
}
