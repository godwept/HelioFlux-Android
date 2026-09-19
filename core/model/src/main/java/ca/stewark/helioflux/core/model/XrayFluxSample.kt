package ca.stewark.helioflux.core.model

data class XrayFluxSample(
    val timestampMillis: Long,
    val goes18Short: Double?,
    val goes18Long: Double?,
    val goes19Short: Double?,
    val goes19Long: Double?,
)
