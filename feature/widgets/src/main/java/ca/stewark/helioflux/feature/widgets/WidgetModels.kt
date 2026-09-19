package ca.stewark.helioflux.feature.widgets

import ca.stewark.helioflux.core.model.DataFreshness

data class SpaceWeatherWidgetModel(
    val kp: String,
    val bz: String,
    val speed: String,
    val flareStatus: String,
    val freshness: DataFreshness,
)

data class AuroraConditionsWidgetModel(
    val kp: String,
    val ovationSummary: String,
    val hemisphericPower: String?,
    val freshness: DataFreshness,
)
