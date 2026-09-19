package ca.stewark.helioflux.core.model

data class FlareEvent(
    val id: String,
    val flareClass: String,
    val timestampMillis: Long,
    val observatory: String,
    val region: String?,
    val location: String?,
)
