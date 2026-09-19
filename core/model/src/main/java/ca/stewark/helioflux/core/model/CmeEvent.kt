package ca.stewark.helioflux.core.model

data class CmeEvent(
    val id: String,
    val timestampMillis: Long,
    val speed: Double?,
    val halfAngle: Double?,
    val direction: String?,
    val type: String?,
    val link: String?,
)
