package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.helioflux.core.model.XrayFluxSample
import ca.stewark.helioflux.ui.components.*
import kotlin.math.log10
const val XrayMinFlux=1e-9
const val XrayMaxFlux=1e-2
val XrayReferenceFluxes=listOf(1e-6,1e-5,1e-4)
fun xrayLogValue(value:Double?):Double?=value?.takeIf{it>0}?.coerceIn(XrayMinFlux,XrayMaxFlux)?.let(::log10)
@Composable fun XrayChart(samples:List<XrayFluxSample>,modifier:Modifier=Modifier){fun series(value:(XrayFluxSample)->Double?)=LineChartSeries(samples.map{ChartPoint(it.timestampMillis.toDouble(),xrayLogValue(value(it)))});HelioFluxLineChart(listOf(series{it.goes18Short},series{it.goes18Long},series{it.goes19Short},series{it.goes19Long}),modifier,XrayReferenceFluxes.map{ChartReferenceLine(log10(it))})}
