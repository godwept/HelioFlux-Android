package ca.stewark.helioflux.core.model

data class SolarWindMag(
    val timestampMillis: Long,
    val bx: Double?,
    val by: Double?,
    val bz: Double?,
    val bt: Double?,
)
