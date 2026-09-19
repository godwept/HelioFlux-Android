package ca.stewark.helioflux.core.model

data class SolarImage(
    val type: SolarImageType,
    val sourceTimestampMillis: Long?,
    val url: String,
)
