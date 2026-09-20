package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.feature.globe.AuroraGlobe
import ca.stewark.helioflux.ui.components.*

private fun <T> RepositoryState<T>.screenData():T?=when(this){is RepositoryState.Available->data;is RepositoryState.Failure->retainedData;else->null}
data class AuroraGlobePresentation(val points:List<AuroraPoint>,val freshness:DataFreshness?)
fun auroraGlobePresentation(state:RepositoryState<AuroraSnapshot>):AuroraGlobePresentation{val snapshot=state.screenData();val freshness=when(state){is RepositoryState.Available->state.freshness;is RepositoryState.Empty->state.freshness;is RepositoryState.Failure->state.freshness;RepositoryState.Loading->null};return AuroraGlobePresentation(snapshot?.points.orEmpty(),freshness)}

enum class SpaceWeatherBlock{AuroraHero,SolarWindHeading,Metrics,Timeframe,BzBt,Density,Speed,Temperature,Goes,GeomagneticHeading,Kp,HemisphericPower}
fun spaceWeatherBlocks(expanded:Boolean):List<List<SpaceWeatherBlock>>{
 val head=listOf(listOf(SpaceWeatherBlock.AuroraHero),listOf(SpaceWeatherBlock.SolarWindHeading),listOf(SpaceWeatherBlock.Metrics),listOf(SpaceWeatherBlock.Timeframe))
 val solar=if(expanded)listOf(listOf(SpaceWeatherBlock.BzBt,SpaceWeatherBlock.Density),listOf(SpaceWeatherBlock.Speed,SpaceWeatherBlock.Temperature),listOf(SpaceWeatherBlock.Goes)) else listOf(SpaceWeatherBlock.BzBt,SpaceWeatherBlock.Density,SpaceWeatherBlock.Speed,SpaceWeatherBlock.Temperature,SpaceWeatherBlock.Goes).map{listOf(it)}
 val geo=listOf(listOf(SpaceWeatherBlock.GeomagneticHeading))+(if(expanded)listOf(listOf(SpaceWeatherBlock.Kp,SpaceWeatherBlock.HemisphericPower)) else listOf(listOf(SpaceWeatherBlock.Kp),listOf(SpaceWeatherBlock.HemisphericPower)))
 return head+solar+geo
}

@Composable fun SpaceWeatherScreen(state:SpaceWeatherUiState,expanded:Boolean,onTimeframe:(Timeframe)->Unit,modifier:Modifier=Modifier,nowMillis:Long=System.currentTimeMillis()){
 val magnetic=state.magnetic.screenData().orEmpty();val plasma=state.plasma.screenData().orEmpty();val kp=state.kp.screenData().orEmpty();val goes=state.goesMagnetometer.screenData();val hp=state.hemisphericPower.screenData().orEmpty();val aurora=auroraGlobePresentation(state.aurora)
 val blocks:Map<SpaceWeatherBlock,@Composable ()->Unit> = mapOf(
  SpaceWeatherBlock.AuroraHero to {{Card(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(16.dp)).testTag("aurora-globe-slot")){Box(Modifier.fillMaxSize()){AuroraGlobe(modifier=Modifier.fillMaxSize(),points=aurora.points);aurora.freshness?.let{Box(Modifier.align(Alignment.TopEnd).padding(10.dp)){FreshnessIndicator(it)}}}}}},
  SpaceWeatherBlock.SolarWindHeading to {{SectionHeading("Solar Wind")}},
  SpaceWeatherBlock.Metrics to {{SolarWindMetrics(latestSolarWindMetrics(magnetic,plasma))}},
  SpaceWeatherBlock.Timeframe to {{TimeframeSelector(state.timeframe,onTimeframe)}},
  SpaceWeatherBlock.BzBt to {{SpaceWeatherLineCard(bzBtChartMeta,bzBtSeries(magnetic,state.timeframe,nowMillis),referenceLines=listOf(ChartReferenceLine(0.0)))}},
  SpaceWeatherBlock.Density to {{SpaceWeatherLineCard(densityChartMeta,plasmaSeries(plasma,state.timeframe,nowMillis){it.density})}},
  SpaceWeatherBlock.Speed to {{SpaceWeatherLineCard(speedChartMeta,plasmaSeries(plasma,state.timeframe,nowMillis){it.speed})}},
  SpaceWeatherBlock.Temperature to {{SpaceWeatherLineCard(temperatureChartMeta,plasmaSeries(plasma,state.timeframe,nowMillis){it.temperature})}},
  SpaceWeatherBlock.Goes to {{SpaceWeatherLineCard(goesChartMeta(goes?.primaryLabel,goes?.secondaryLabel),goesSeriesOrEmpty(goes,state.timeframe,nowMillis))}},
  SpaceWeatherBlock.GeomagneticHeading to {{SectionHeading("Geomagnetic Activity")}},
  SpaceWeatherBlock.Kp to {{KpChart(kpPresentation(kp,state.timeframe,nowMillis))}},
  SpaceWeatherBlock.HemisphericPower to {{HemisphericPowerChart(hemisphericPowerSeries(hp,state.timeframe,nowMillis))}},
 )
 LazyColumn(modifier.testTag("space-weather-screen").padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  spaceWeatherBlocks(expanded).forEach{row->item{
   if(row.size==1)Box(Modifier.fillMaxWidth().testTag(if(expanded)"space-weather-expanded" else "space-weather-compact")){blocks.getValue(row.first())()}
   else Row(Modifier.fillMaxWidth().testTag("space-weather-expanded"),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{block->Box(Modifier.weight(1f)){blocks.getValue(block)()}}}
  }}
 }
}
@Composable private fun SectionHeading(text:String){Text(text,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onBackground,modifier=Modifier.padding(top=8.dp,bottom=2.dp))}
