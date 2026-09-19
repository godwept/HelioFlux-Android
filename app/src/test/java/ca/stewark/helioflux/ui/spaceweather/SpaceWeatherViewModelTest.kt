package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
class SpaceWeatherViewModelTest{
 @Test fun combinedCachedStateIncludesMetricsTimeframeSeriesAuroraAndFreshness()=runBlocking{
  val source=DataSourceKey("cached");val freshness=DataFreshness.Cached
  val magnetic=listOf(SolarWindMag(1,null,null,-4.5,6.0));val plasma=listOf(SolarWindPlasma(1,7.2,455.0,100000.0));val kp=listOf(KpSample(1,5.0));val hp=listOf(HemisphericPowerSample(1,42.0,38.0));val goes=GoesMagnetometerSeries(listOf(GoesMagSample(1,101.0,99.0)),"GOES-19","GOES-18");val aurora=AuroraSnapshot(1,2,emptyList())
  fun <T> cached(data:T):RepositoryState<T> = RepositoryState.Failure(source,"offline",data,freshness)
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val vm=SpaceWeatherViewModel.forTest(flowOf(cached(magnetic)),flowOf(cached(plasma)),flowOf(cached(kp)),flowOf(cached(goes)),flowOf(cached(hp)),flowOf(cached(aurora)),scope)
  yield();vm.selectTimeframe(Timeframe.TwelveHours);yield();val state=vm.state.value
  assertEquals(-4.5,state.currentBz!!,0.0);assertEquals(455.0,state.currentSpeed!!,0.0);assertEquals(7.2,state.currentDensity!!,0.0);assertEquals(Timeframe.TwelveHours,state.timeframe)
  assertEquals(magnetic,(state.magnetic as RepositoryState.Failure).retainedData);assertEquals(kp,(state.kp as RepositoryState.Failure).retainedData);assertEquals(hp,(state.hemisphericPower as RepositoryState.Failure).retainedData);assertEquals(goes,(state.goesMagnetometer as RepositoryState.Failure).retainedData);assertEquals(aurora,(state.aurora as RepositoryState.Failure).retainedData);assertEquals(DataFreshness.Cached,state.freshness[source]);scope.cancel()
 }
}
