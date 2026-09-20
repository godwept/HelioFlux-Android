package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.HemisphericPowerSample
import ca.stewark.helioflux.ui.components.*

val hemisphericPowerMeta=ChartCardMeta("OVATION Model · Today","Hemispheric Power (GW)",listOf("North","South"))
const val hemisphericPowerEmptyMessage="No hemispheric power data"
fun hemisphericPowerSeries(samples:List<HemisphericPowerSample>,timeframe:Timeframe,now:Long)=filterByTimeframe(samples,timeframe,now){it.timestampMillis}.let{listOf(LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.north)},ChartSeriesStyle.Secondary),LineChartSeries(it.map{s->ChartPoint(s.timestampMillis.toDouble(),s.south)},ChartSeriesStyle.Purple))}
@Composable fun HemisphericPowerChart(series:List<LineChartSeries>,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(hemisphericPowerMeta.context.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(hemisphericPowerMeta.title,style=MaterialTheme.typography.titleMedium);LegendRow(hemisphericPowerMeta.legendLabels,series.map{seriesColor(it.style)});if(series.all{it.points.none{p->p.y!=null}})Text(hemisphericPowerEmptyMessage) else HelioFluxLineChart(series,Modifier.fillMaxWidth().height(180.dp))}}}
