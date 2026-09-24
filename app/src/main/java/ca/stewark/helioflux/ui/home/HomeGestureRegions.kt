package ca.stewark.helioflux.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

class HomeGestureRegions {
    private val foreground = mutableMapOf<String, Rect>()

    fun update(id: String, bounds: Rect) {
        foreground[id] = bounds
    }

    fun remove(id: String) {
        foreground.remove(id)
    }

    fun isExposed(point: Offset): Boolean = foreground.values.none { point in it }
}

@Composable
internal fun homeRegionModifier(
    id: String,
    onBounds: ((String, Rect?) -> Unit)?,
): Modifier {
    DisposableEffect(id, onBounds) {
        onDispose { onBounds?.invoke(id, null) }
    }
    return if (onBounds == null) Modifier else Modifier.onGloballyPositioned {
        onBounds(id, it.boundsInRoot())
    }
}
