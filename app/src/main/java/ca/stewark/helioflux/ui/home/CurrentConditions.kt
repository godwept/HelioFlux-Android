package ca.stewark.helioflux.ui.home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import java.util.Locale
@Composable fun CurrentConditions(conditions:HomeConditions,onDestination:(HelioFluxDestination)->Unit,modifier:Modifier=Modifier){
 Card(modifier.testTag("current-conditions")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Current Conditions",style=MaterialTheme.typography.titleLarge)
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("Kp",conditions.kp?.oneDecimal()?:"—",Modifier.weight(1f)){onDestination(HelioFluxDestination.SpaceWeather)};Metric("Bz",conditions.bz?.let{it.oneDecimal()+" nT"}?:"—",Modifier.weight(1f)){onDestination(HelioFluxDestination.SpaceWeather)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("Wind",conditions.speed?.let{it.oneDecimal()+" km/s"}?:"—",Modifier.weight(1f)){onDestination(HelioFluxDestination.SpaceWeather)};val flare=conditions.flare;Metric("Flare",flare?.let{"M "+it.m+"% · X "+it.x+"%"}?:"—",Modifier.weight(1f)){onDestination(HelioFluxDestination.SolarActivity)}}
  val kp=conditions.kp;Text(if(kp==null)"Aurora / geomagnetic status unavailable" else if(kp>=5)"Geomagnetic storm conditions" else "Geomagnetic conditions below storm level")
 }}
}
@Composable private fun Metric(label:String,value:String,modifier:Modifier,onClick:()->Unit){Surface(modifier.clickable(onClick=onClick).testTag("metric-"+label.lowercase()),tonalElevation=2.dp,shape=MaterialTheme.shapes.medium){Column(Modifier.padding(12.dp)){Text(label,style=MaterialTheme.typography.labelMedium);Text(value,style=MaterialTheme.typography.titleMedium)}}}
private fun Double.oneDecimal()=String.format(Locale.US,"%.1f",this)
