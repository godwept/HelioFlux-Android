package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.AceEpamSample
import ca.stewark.helioflux.ui.components.*
fun exponentialAxisLabel(value:Double):String="%.1e".format(value)
@Composable fun AceEpamChart(samples:List<AceEpamSample>,modifier:Modifier=Modifier){if(samples.isEmpty()){Text("ACE EPAM data unavailable",modifier);return};fun series(value:(AceEpamSample)->Double?)=LineChartSeries(samples.map{ChartPoint(it.timestampMillis.toDouble(),value(it))});Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)){Text("ACE EPAM");HelioFluxLineChart(listOf(series{it.electronLow},series{it.protonLow},series{it.protonMid}),Modifier.fillMaxWidth().height(240.dp))}}
