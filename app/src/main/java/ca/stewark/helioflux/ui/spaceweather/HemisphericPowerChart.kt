package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.HemisphericPowerSample
import ca.stewark.helioflux.ui.components.*
fun hemisphericPowerSeries(samples:List<HemisphericPowerSample>,timeframe:Timeframe,now:Long)=filterByTimeframe(samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.north)}),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.south)}))}
@Composable fun HemisphericPowerChart(series:List<LineChartSeries>,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Text("Hemispheric Power",style=MaterialTheme.typography.titleMedium);if(series.all{it.points.none{p->p.y!=null}})Text("No hemispheric power data") else HelioFluxLineChart(series,Modifier.fillMaxWidth().height(180.dp))}}}
