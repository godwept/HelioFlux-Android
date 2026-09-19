package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.CmeEvent
enum class CmeSpeedEmphasis{Normal,Elevated,Strong}
fun cmeSpeedEmphasis(speed:Double?):CmeSpeedEmphasis=when{speed==null||speed<500->CmeSpeedEmphasis.Normal;speed<1000->CmeSpeedEmphasis.Elevated;else->CmeSpeedEmphasis.Strong}
@Composable fun CmeList(cmes:List<CmeEvent>,onDetails:(CmeEvent)->Unit,modifier:Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Recent CMEs",style=MaterialTheme.typography.titleLarge);if(cmes.isEmpty())Text("No recent CMEs");cmes.sortedByDescending{it.timestampMillis}.forEach{cme->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(cme.speed?.let{it.toInt().toString()+" km/s"}?:"Speed unavailable",style=MaterialTheme.typography.titleMedium);Text(cme.timestampMillis.toString()+(cme.direction?.let{" · "+it}?:"")+(cme.halfAngle?.let{" · width "+(it*2).toInt()+"°"}?:""));if(cme.link!=null)TextButton(onClick={onDetails(cme)}){Text("Details")}}}}}}
