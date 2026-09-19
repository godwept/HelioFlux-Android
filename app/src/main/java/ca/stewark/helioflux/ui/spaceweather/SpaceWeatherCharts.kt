package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.GoesMagnetometerSeries
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.*
fun bzBtSeries(samples:List<SolarWindMag>,timeframe:Timeframe,now:Long)=filterByTimeframe(samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.bz)}),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.bt)}))}
fun plasmaSeries(samples:List<SolarWindPlasma>,timeframe:Timeframe,now:Long,field:(SolarWindPlasma)->Double?)=listOf(LineChartSeries(filterByTimeframe(samples,timeframe,now){it.timestampMillis}.map{ChartPoint(it.timestampMillis.toDouble(),field(it))}))
fun goesSeries(series:GoesMagnetometerSeries,timeframe:Timeframe,now:Long)=filterByTimeframe(series.samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.primary)}),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.secondary)}))}
@Composable fun SpaceWeatherLineCard(title:String,series:List<LineChartSeries>,modifier:Modifier=Modifier,referenceLines:List<ChartReferenceLine> = emptyList()){Card(modifier){Column(Modifier.padding(12.dp)){Text(title,style=MaterialTheme.typography.titleMedium);HelioFluxLineChart(series,Modifier.fillMaxWidth().height(180.dp),referenceLines)}}}
