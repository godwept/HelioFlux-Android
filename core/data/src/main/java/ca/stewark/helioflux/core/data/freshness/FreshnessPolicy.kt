package ca.stewark.helioflux.core.data.freshness

data class FreshnessPolicy(val freshForMillis: Long)

enum class FreshnessSource { REALTIME_NOAA, OVATION, HELIOVIEWER, FORECAST_DISCUSSION, DONKI_EVENTS, IMAGERY_METADATA }

object FreshnessPolicies {
 private const val MINUTE=60_000L
 private const val HOUR=60*MINUTE
 val all=mapOf(
  FreshnessSource.REALTIME_NOAA to FreshnessPolicy(10*MINUTE),
  FreshnessSource.OVATION to FreshnessPolicy(10*MINUTE),
  FreshnessSource.HELIOVIEWER to FreshnessPolicy(6*HOUR),
  FreshnessSource.FORECAST_DISCUSSION to FreshnessPolicy(6*HOUR),
  FreshnessSource.DONKI_EVENTS to FreshnessPolicy(30*MINUTE),
  FreshnessSource.IMAGERY_METADATA to FreshnessPolicy(5*MINUTE),
 )
}
