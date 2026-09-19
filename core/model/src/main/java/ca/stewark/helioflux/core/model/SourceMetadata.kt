package ca.stewark.helioflux.core.model

data class SourceMetadata(
    val source: DataSourceKey,
    val observationTimestampMillis: Long?,
    val fetchedTimestampMillis: Long,
    val freshness: DataFreshness,
)
