package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.GoesMagnetometerSeries
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.*
import ca.stewark.helioflux.ui.theme.*

data class ChartCardMeta(val context:String,val title:String,val legendLabels:List<String> = emptyList())
val bzBtChartMeta=ChartCardMeta("Magnetic Field","IMF Bz / Bt (nT)",listOf("Bz","Bt"))
val densityChartMeta=ChartCardMeta("Solar Wind","Density (p/cm³)")
val speedChartMeta=ChartCardMeta("Solar Wind","Speed (km/s)")
val temperatureChartMeta=ChartCardMeta("Solar Wind","Temperature (K)")
fun goesChartMeta(primary:String?,secondary:String?)=ChartCardMeta("GOES Magnetometer","Magnetic Field (nT)",listOfNotNull(primary,secondary))

fun bzBtSeries(samples:List<SolarWindMag>,timeframe:Timeframe,now:Long)=filterByTimeframe(samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.bz)},ChartSeriesStyle.Bz),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.bt)},ChartSeriesStyle.Secondary))}
fun plasmaSeries(samples:List<SolarWindPlasma>,timeframe:Timeframe,now:Long,field:(SolarWindPlasma)->Double?)=listOf(LineChartSeries(filterByTimeframe(samples,timeframe,now){it.timestampMillis}.map{ChartPoint(it.timestampMillis.toDouble(),field(it))}))
fun goesSeries(series:GoesMagnetometerSeries,timeframe:Timeframe,now:Long)=filterByTimeframe(series.samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.primary)},ChartSeriesStyle.Primary),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.secondary)},ChartSeriesStyle.Alert))}
fun goesSeriesOrEmpty(series:GoesMagnetometerSeries?,timeframe:Timeframe,now:Long)=series?.let{goesSeries(it,timeframe,now)}?:emptyList()

@Composable fun SpaceWeatherLineCard(meta:ChartCardMeta,series:List<LineChartSeries>,modifier:Modifier=Modifier,referenceLines:List<ChartReferenceLine> = emptyList()){
 Card(modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(meta.context.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(meta.title,style=MaterialTheme.typography.titleMedium)
   if(meta.legendLabels.isNotEmpty())LegendRow(meta.legendLabels,series.map{seriesColor(it.style)})
   HelioFluxLineChart(series,Modifier.fillMaxWidth().height(180.dp),referenceLines)
  }
 }
}
@Composable fun LegendRow(labels:List<String>,colors:List<Color>){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){labels.forEachIndexed{i,label->Text("● $label",style=MaterialTheme.typography.labelSmall,color=colors.getOrElse(i){DataCyan})}}}
