package ca.stewark.helioflux.core.data.freshness

import ca.stewark.helioflux.core.model.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshnessEvaluatorTest {
 @Test fun recentSuccessfulFetchIsFresh(){ assertEquals(FreshnessResult.Available(DataFreshness.Fresh),FreshnessEvaluator.evaluate(1_000,900,950,true,true,FreshnessPolicy(200))) }
 @Test fun oldSuccessfulDataIsDelayed(){ assertEquals(FreshnessResult.Available(DataFreshness.Delayed),FreshnessEvaluator.evaluate(1_000,700,950,true,true,FreshnessPolicy(200))) }
 @Test fun failedRefreshWithCacheIsCached(){ assertEquals(FreshnessResult.Available(DataFreshness.Cached),FreshnessEvaluator.evaluate(1_000,900,950,false,true,FreshnessPolicy(200))) }
 @Test fun noCacheIsUnavailable(){ assertTrue(FreshnessEvaluator.evaluate(1_000,null,null,false,false,FreshnessPolicy(200)) is FreshnessResult.Unavailable) }
 @Test fun everyRequiredSourceHasCentralPolicy(){ assertEquals(FreshnessSource.entries.toSet(),FreshnessPolicies.all.keys) }
 @Test fun ovationMatchesPwaTenMinuteCacheAndHelioviewerSixHourStaleLimit(){ assertEquals(600_000L,FreshnessPolicies.all.getValue(FreshnessSource.OVATION).freshForMillis); assertEquals(21_600_000L,FreshnessPolicies.all.getValue(FreshnessSource.HELIOVIEWER).freshForMillis) }
}
