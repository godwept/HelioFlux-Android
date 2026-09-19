package ca.stewark.helioflux.core.model

data class SolarWindPlasma(
    val timestampMillis: Long,
    val density: Double?,
    val speed: Double?,
    val temperature: Double?,
)
