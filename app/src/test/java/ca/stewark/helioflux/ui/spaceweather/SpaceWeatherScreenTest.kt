package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.AuroraPoint
import ca.stewark.helioflux.core.model.AuroraSnapshot
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceWeatherScreenTest {
 @Test fun compactStacksAndExpandedUsesTwoColumns(){assertEquals(1,spaceWeatherColumnCount(false));assertEquals(2,spaceWeatherColumnCount(true))}

 @Test fun globeReceivesNormalizedSnapshotAndCachedFreshnessOnly(){
  val snapshot=AuroraSnapshot(1L,2L,listOf(AuroraPoint(65.0,-50.0,26.0)))
  val presentation=auroraGlobePresentation(RepositoryState.Available(DataSourceKey("ovation"),snapshot,DataFreshness.Cached))
  assertEquals(snapshot.points,presentation.points)
  assertEquals(DataFreshness.Cached,presentation.freshness)
 }

 @Test fun unavailableAuroraDoesNotRemoveRestOfScreenData(){
  val presentation=auroraGlobePresentation(RepositoryState.Loading)
  assertTrue(presentation.points.isEmpty())
  assertEquals(null,presentation.freshness)
 }
}
