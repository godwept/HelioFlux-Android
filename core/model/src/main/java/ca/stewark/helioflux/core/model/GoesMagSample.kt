package ca.stewark.helioflux.core.model

data class GoesMagSample(
    val timestampMillis: Long,
    val primary: Double?,
    val secondary: Double?,
)
