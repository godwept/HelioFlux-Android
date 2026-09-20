package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.SolarWindMag
import ca.stewark.helioflux.core.model.SolarWindPlasma
import ca.stewark.helioflux.ui.theme.*

data class SolarWindMetricValues(val bz:Double?,val speed:Double?,val density:Double?)
data class SolarWindMetricPresentation(val label:String,val value:String,val tag:String)

fun latestSolarWindMetrics(magnetic:List<SolarWindMag>,plasma:List<SolarWindPlasma>)=SolarWindMetricValues(magnetic.lastOrNull{it.bz!=null}?.bz,plasma.lastOrNull{it.speed!=null}?.speed,plasma.lastOrNull{it.density!=null}?.density)
fun solarWindMetricPresentation(values:SolarWindMetricValues)=listOf(
 SolarWindMetricPresentation("Bz",values.bz?.let{"%.1f nT".format(it)}?:"—","metric-bz"),
 SolarWindMetricPresentation("Speed",values.speed?.let{"%.0f km/s".format(it)}?:"—","metric-speed"),
 SolarWindMetricPresentation("Density",values.density?.let{"%.1f p/cm³".format(it)}?:"—","metric-density"),
)

@Composable fun SolarWindMetrics(values:SolarWindMetricValues,modifier:Modifier=Modifier){
 val items=solarWindMetricPresentation(values)
 Row(modifier.fillMaxWidth().testTag("solar-wind-metrics"),horizontalArrangement=Arrangement.spacedBy(8.dp)){
  items.forEachIndexed{index,item->MetricPill(item,metricColor(index),Modifier.weight(1f))}
 }
}
@Composable private fun MetricPill(item:SolarWindMetricPresentation,color:Color,modifier:Modifier){
 Column(modifier.border(BorderStroke(1.dp,color.copy(alpha=.7f)),RoundedCornerShape(18.dp)).padding(horizontal=10.dp,vertical=8.dp).testTag(item.tag)){
  Text(item.label,style=MaterialTheme.typography.labelSmall,color=color)
  Text(item.value,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurface,maxLines=1)
 }
}
private fun metricColor(index:Int)=when(index){0->AlertRed;1->FreshGreen;else->SolarOrange}
