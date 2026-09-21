package ca.stewark.helioflux.core.model

data class AceEpamSample(
    val timestampMillis: Long,
    val electronLow: Double?,
    val electronHigh: Double?,
    val protonLow: Double?,
    val protonMid: Double?,
    val protonHigh: Double?,
    val protonFp6: Double?,
    val protonP7: Double?,
)
