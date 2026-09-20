package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.SolarWindMag
import ca.stewark.helioflux.core.model.SolarWindPlasma
import ca.stewark.helioflux.ui.theme.*
import java.util.Locale

data class SolarWindMetricValues(val bz:Double?,val speed:Double?,val density:Double?)
data class SolarWindMetricPresentation(val label:String,val value:String,val tag:String,val accent:Color)

fun latestSolarWindMetrics(magnetic:List<SolarWindMag>,plasma:List<SolarWindPlasma>)=SolarWindMetricValues(magnetic.lastOrNull{it.bz!=null}?.bz,plasma.lastOrNull{it.speed!=null}?.speed,plasma.lastOrNull{it.density!=null}?.density)
fun solarWindMetricPresentation(values:SolarWindMetricValues)=listOf(
 SolarWindMetricPresentation("Bz",values.bz?.let{String.format(Locale.US,"%.1f nT",it)}?:"—","metric-bz",DataCyan),
 SolarWindMetricPresentation("SPD",values.speed?.let{String.format(Locale.US,"%.0f km/s",it)}?:"—","metric-speed",FreshGreen),
 SolarWindMetricPresentation("DEN",values.density?.let{String.format(Locale.US,"%.1f p/cm³",it)}?:"—","metric-density",SolarOrange),
)

@Composable fun SolarWindMetrics(values:SolarWindMetricValues,modifier:Modifier=Modifier){
 Row(modifier.fillMaxWidth().testTag("solar-wind-metrics"),horizontalArrangement=Arrangement.spacedBy(6.dp)){
  solarWindMetricPresentation(values).forEach{item->MetricPill(item,Modifier.weight(1f))}
 }
}
@Composable private fun MetricPill(item:SolarWindMetricPresentation,modifier:Modifier){
 val shape=RoundedCornerShape(50)
 Surface(
  modifier.shadow(elevation=5.dp,shape=shape,ambientColor=item.accent.copy(alpha=.28f),spotColor=item.accent.copy(alpha=.38f)).testTag(item.tag),
  color=SpaceSurface,
  shape=shape,
  border=BorderStroke(1.dp,item.accent.copy(alpha=.52f)),
 ){
  Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
   Text(item.label.uppercase(),style=MaterialTheme.typography.labelSmall,color=item.accent)
   Text(item.value,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold,maxLines=1)
  }
 }
}
