package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.HelioFluxBarChart
data class KpPresentation(val values:List<Pair<Double,Double>>,val currentStatus:KpStatus?)
fun kpPresentation(samples:List<KpSample>,timeframe:Timeframe,now:Long):KpPresentation{
 val available=samples.mapNotNull{s->s.kp?.let{s.timestampMillis.toDouble() to it}}
 val filtered=available.filter{it.first>=now-timeframe.durationMillis}
 val visible=if(filtered.isNotEmpty())filtered else available.takeLast(1)
 return KpPresentation(visible,available.lastOrNull()?.second?.let(::kpStatus))
}
@Composable fun KpChart(presentation:KpPresentation,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Text("Planetary Kp",style=MaterialTheme.typography.titleMedium);Text(presentation.currentStatus?.name?:"No current Kp");HelioFluxBarChart(presentation.values,Modifier.fillMaxWidth().height(180.dp))}}}
