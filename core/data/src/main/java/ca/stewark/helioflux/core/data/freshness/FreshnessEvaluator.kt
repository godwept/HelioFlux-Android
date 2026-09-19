package ca.stewark.helioflux.core.data.freshness

import ca.stewark.helioflux.core.model.DataFreshness

sealed interface FreshnessResult {
 data class Available(val freshness: DataFreshness): FreshnessResult
 data object Unavailable: FreshnessResult
}

object FreshnessEvaluator {
 fun evaluate(nowMillis:Long, observationTimestampMillis:Long?, fetchedTimestampMillis:Long?, refreshSucceeded:Boolean, hasCache:Boolean, policy:FreshnessPolicy):FreshnessResult {
  if(!hasCache || fetchedTimestampMillis==null) return FreshnessResult.Unavailable
  if(!refreshSucceeded) return FreshnessResult.Available(DataFreshness.Cached)
  val timestamp=observationTimestampMillis ?: fetchedTimestampMillis
  return FreshnessResult.Available(if(nowMillis-timestamp<=policy.freshForMillis) DataFreshness.Fresh else DataFreshness.Delayed)
 }
}
