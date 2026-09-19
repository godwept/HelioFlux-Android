package ca.stewark.helioflux.core.model

data class ForecastSection(
    val key: String,
    val title: String,
    val summary: String,
    val forecast: String,
    val issueTime: String?,
)
