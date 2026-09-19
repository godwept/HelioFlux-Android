package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.FlareEvent
fun flareClassGroup(value:String):Char=value.firstOrNull()?.uppercaseChar()?.takeIf{it in "ABCMX"}?:'A'
@Composable fun FlareList(flares:List<FlareEvent>,modifier:Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Recent flares",style=MaterialTheme.typography.titleLarge);if(flares.isEmpty())Text("No recent flares");flares.sortedByDescending{it.timestampMillis}.forEach{flare->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(flare.flareClass,style=MaterialTheme.typography.titleMedium);Text(flare.timestampMillis.toString()+" · "+flare.observatory);flare.region?.let{Text("AR "+it)};flare.location?.let{Text(it)}}}}}}
