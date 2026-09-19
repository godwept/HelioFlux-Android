package ca.stewark.helioflux.core.model

data class AuroraSnapshot(
    val observationTimestampMillis: Long,
    val forecastTimestampMillis: Long,
    val points: List<AuroraPoint>,
)
