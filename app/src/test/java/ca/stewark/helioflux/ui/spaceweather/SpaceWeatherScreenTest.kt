package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceWeatherScreenTest {
 @Test fun compactHierarchyMatchesApprovedDesign(){
  assertEquals(listOf(
   SpaceWeatherBlock.AuroraHero,SpaceWeatherBlock.SolarWindHeading,SpaceWeatherBlock.Metrics,SpaceWeatherBlock.Timeframe,
   SpaceWeatherBlock.BzBt,SpaceWeatherBlock.Density,SpaceWeatherBlock.Speed,SpaceWeatherBlock.Temperature,SpaceWeatherBlock.Goes,
   SpaceWeatherBlock.GeomagneticHeading,SpaceWeatherBlock.Kp,SpaceWeatherBlock.HemisphericPower
  ),spaceWeatherBlocks(false).flatten())
 }
 @Test fun expandedGroupsChartsWithinSectionsOnly(){
  val rows=spaceWeatherBlocks(true)
  assertEquals(listOf(SpaceWeatherBlock.AuroraHero),rows[0])
  assertTrue(rows.contains(listOf(SpaceWeatherBlock.BzBt,SpaceWeatherBlock.Density)))
  assertTrue(rows.contains(listOf(SpaceWeatherBlock.Speed,SpaceWeatherBlock.Temperature)))
  assertTrue(rows.contains(listOf(SpaceWeatherBlock.Goes)))
  assertTrue(rows.contains(listOf(SpaceWeatherBlock.Kp,SpaceWeatherBlock.HemisphericPower)))
  assertTrue(rows.none{it.contains(SpaceWeatherBlock.Goes)&&it.contains(SpaceWeatherBlock.Kp)})
 }
 @Test fun globeReceivesNormalizedSnapshotAndCachedFreshnessOnly(){
  val snapshot=AuroraSnapshot(1L,2L,listOf(AuroraPoint(65.0,-50.0,26.0)))
  val presentation=auroraGlobePresentation(RepositoryState.Available(snapshot,DataSourceKey("ovation"),DataFreshness.Cached))
  assertEquals(snapshot.points,presentation.points);assertEquals(DataFreshness.Cached,presentation.freshness)
 }
 @Test fun unavailableAuroraDoesNotRemoveRestOfScreenData(){val presentation=auroraGlobePresentation(RepositoryState.Loading);assertTrue(presentation.points.isEmpty());assertEquals(null,presentation.freshness);assertTrue(spaceWeatherBlocks(false).flatten().contains(SpaceWeatherBlock.SolarWindHeading))}

 @Test fun globeTouchTemporarilyPreventsLazyColumnFromCancellingSceneViewGesture(){
  val source=File("src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt").readText()
  assertTrue(source.contains("var globeTouchActive"))
  assertTrue(source.contains("userScrollEnabled = !globeTouchActive"))
  assertTrue(source.contains("{ globeTouchActive = it }"))
 }
}
