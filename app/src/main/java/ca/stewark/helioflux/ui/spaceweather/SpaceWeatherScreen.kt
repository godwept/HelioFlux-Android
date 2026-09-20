package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.feature.globe.AuroraGlobe
import ca.stewark.helioflux.ui.components.*

fun spaceWeatherColumnCount(expanded:Boolean)=if(expanded)2 else 1
private fun <T> RepositoryState<T>.screenData():T?=when(this){is RepositoryState.Available->data;is RepositoryState.Failure->retainedData;else->null}

data class AuroraGlobePresentation(
 val points: List<AuroraPoint>,
 val freshness: DataFreshness?,
)

fun auroraGlobePresentation(state: RepositoryState<AuroraSnapshot>): AuroraGlobePresentation {
 val snapshot=state.screenData()
 val freshness=when(state){is RepositoryState.Available->state.freshness;is RepositoryState.Empty->state.freshness;is RepositoryState.Failure->state.freshness;RepositoryState.Loading->null}
 return AuroraGlobePresentation(snapshot?.points.orEmpty(),freshness)
}

@Composable fun SpaceWeatherScreen(state:SpaceWeatherUiState,expanded:Boolean,onTimeframe:(Timeframe)->Unit,modifier:Modifier=Modifier,nowMillis:Long=System.currentTimeMillis()){
 val magnetic=state.magnetic.screenData().orEmpty();val plasma=state.plasma.screenData().orEmpty();val kp=state.kp.screenData().orEmpty();val goes=state.goesMagnetometer.screenData();val hp=state.hemisphericPower.screenData().orEmpty();val aurora=auroraGlobePresentation(state.aurora)
 val charts=listOf<@Composable ()->Unit>(
  {SpaceWeatherLineCard("IMF Bz / Bt",bzBtSeries(magnetic,state.timeframe,nowMillis),referenceLines=listOf(ChartReferenceLine(0.0)))},
  {SpaceWeatherLineCard("Solar Wind Density",plasmaSeries(plasma,state.timeframe,nowMillis){it.density})},
  {SpaceWeatherLineCard("Solar Wind Speed",plasmaSeries(plasma,state.timeframe,nowMillis){it.speed})},
  {SpaceWeatherLineCard("Solar Wind Temperature",plasmaSeries(plasma,state.timeframe,nowMillis){it.temperature})},
  {SpaceWeatherLineCard("GOES Magnetometer"+(goes?.primaryLabel?.let{" · "+it}?:""),goes?.let{goesSeries(it,state.timeframe,nowMillis)}?:emptyList())},
  {KpChart(kpPresentation(kp,state.timeframe,nowMillis))},
  {HemisphericPowerChart(hemisphericPowerSeries(hp,state.timeframe,nowMillis))}
 )
 LazyColumn(modifier.testTag("space-weather-screen").padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Space Weather",style=MaterialTheme.typography.headlineSmall);state.magnetic.sourceFreshness()?.let{FreshnessIndicator(it)}}}
  item{TimeframeSelector(state.timeframe,onTimeframe)}
  item{SolarWindMetrics(latestSolarWindMetrics(magnetic,plasma))}
  item{Card(Modifier.fillMaxWidth().height(240.dp).testTag("aurora-globe-slot")){Box(Modifier.fillMaxSize()){AuroraGlobe(modifier=Modifier.fillMaxSize(),points=aurora.points);aurora.freshness?.let{Box(Modifier.align(Alignment.TopEnd).padding(8.dp)){FreshnessIndicator(it)}}}}}
  if(expanded){items((charts.size+1)/2){row->Row(Modifier.fillMaxWidth().testTag("space-weather-expanded"),horizontalArrangement=Arrangement.spacedBy(12.dp)){charts[row*2]( );if(row*2+1<charts.size)Box(Modifier.weight(1f)){charts[row*2+1]()} else Spacer(Modifier.weight(1f))}}}
  else charts.forEach{chart->item{Box(Modifier.fillMaxWidth().testTag("space-weather-compact")){chart()}}}
 }
}
private fun RepositoryState<*>.sourceFreshness():DataFreshness?=when(this){is RepositoryState.Available->freshness;is RepositoryState.Empty->freshness;is RepositoryState.Failure->freshness;RepositoryState.Loading->null}
