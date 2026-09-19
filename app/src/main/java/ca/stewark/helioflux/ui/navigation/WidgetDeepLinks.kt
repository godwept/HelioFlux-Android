package ca.stewark.helioflux.ui.navigation

import android.content.Intent

object WidgetDeepLinks {
    const val EXTRA_DESTINATION = "ca.stewark.helioflux.extra.WIDGET_DESTINATION"
    const val EXTRA_FOCUS = "ca.stewark.helioflux.extra.WIDGET_FOCUS"
    const val FOCUS_AURORA_GLOBE = "aurora-globe"
    const val FOCUS_SUN_HERO = "sun-hero"

    fun destination(intent: Intent?): HelioFluxDestination? = when (intent?.getStringExtra(EXTRA_DESTINATION)) {
        HelioFluxDestination.SpaceWeather.route -> HelioFluxDestination.SpaceWeather
        HelioFluxDestination.Home.route -> HelioFluxDestination.Home
        else -> null
    }

    fun focus(intent: Intent?): String? = intent?.getStringExtra(EXTRA_FOCUS)
}
