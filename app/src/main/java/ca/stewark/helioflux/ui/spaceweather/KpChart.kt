package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.HelioFluxBarChart

data class KpPresentation(val values:List<Pair<Double,Double>>,val currentStatus:KpStatus?)
val kpChartMeta=ChartCardMeta("Geomagnetic Activity","Planetary Kp Index (3-hour)")
fun kpStatusLabel(status:KpStatus?)=status?.name?:"No current Kp"
fun kpPresentation(samples:List<KpSample>,timeframe:Timeframe,now:Long):KpPresentation{
 val available=samples.mapNotNull{s->s.kp?.let{s.timestampMillis.toDouble() to it}}
 val filtered=available.filter{it.first>=now-timeframe.durationMillis}
 val visible=if(filtered.isNotEmpty())filtered else available.takeLast(1)
 return KpPresentation(visible,available.lastOrNull()?.second?.let(::kpStatus))
}
@Composable fun KpChart(presentation:KpPresentation,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(kpChartMeta.context.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(kpChartMeta.title,style=MaterialTheme.typography.titleMedium);Text(kpStatusLabel(presentation.currentStatus),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);HelioFluxBarChart(presentation.values,Modifier.fillMaxWidth().height(180.dp))}}}
