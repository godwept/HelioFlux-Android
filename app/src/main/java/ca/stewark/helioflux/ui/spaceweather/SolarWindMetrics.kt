package ca.stewark.helioflux.ui.spaceweather
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.SolarWindMag
import ca.stewark.helioflux.core.model.SolarWindPlasma
data class SolarWindMetricValues(val bz:Double?,val speed:Double?,val density:Double?)
fun latestSolarWindMetrics(magnetic:List<SolarWindMag>,plasma:List<SolarWindPlasma>)=SolarWindMetricValues(magnetic.lastOrNull{it.bz!=null}?.bz,plasma.lastOrNull{it.speed!=null}?.speed,plasma.lastOrNull{it.density!=null}?.density)
@Composable fun SolarWindMetrics(values:SolarWindMetricValues,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth().testTag("solar-wind-metrics"),horizontalArrangement=Arrangement.spacedBy(8.dp)){
 MetricCard("Bz",values.bz?.let{"%.1f nT".format(it)}?:"—","metric-bz",Modifier.weight(1f));MetricCard("Speed",values.speed?.let{"%.0f km/s".format(it)}?:"—","metric-speed",Modifier.weight(1f));MetricCard("Density",values.density?.let{"%.1f p/cm³".format(it)}?:"—","metric-density",Modifier.weight(1f))
}}
@Composable private fun MetricCard(label:String,value:String,tag:String,modifier:Modifier){Card(modifier.testTag(tag)){Column(Modifier.padding(12.dp)){Text(label);Text(value)}}}
