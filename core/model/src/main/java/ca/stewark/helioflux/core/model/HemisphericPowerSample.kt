package ca.stewark.helioflux.core.model

data class HemisphericPowerSample(
    val timestampMillis: Long,
    val north: Double?,
    val south: Double?,
)
