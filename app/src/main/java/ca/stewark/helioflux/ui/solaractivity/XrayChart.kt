package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.helioflux.core.model.XrayFluxSample
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.ChartReferenceLine
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries
import kotlin.math.log10

const val XrayMinFlux = 1e-9
const val XrayMaxFlux = 1e-2
val XrayReferenceFluxes = listOf(1e-6, 1e-5, 1e-4)

fun xrayLogValue(value: Double?): Double? =
    value?.takeIf { it > 0 }?.coerceIn(XrayMinFlux, XrayMaxFlux)?.let(::log10)

private fun xraySeries(
    samples: List<XrayFluxSample>,
    value: (XrayFluxSample) -> Double?,
): LineChartSeries = LineChartSeries(
    samples.map { sample ->
        ChartPoint(sample.timestampMillis.toDouble(), xrayLogValue(value(sample)))
    },
)

@Composable
fun XrayChart(samples: List<XrayFluxSample>, modifier: Modifier = Modifier) {
    HelioFluxLineChart(
        series = listOf(
            xraySeries(samples) { it.goes18Short },
            xraySeries(samples) { it.goes18Long },
            xraySeries(samples) { it.goes19Short },
            xraySeries(samples) { it.goes19Long },
        ),
        modifier = modifier,
        referenceLines = XrayReferenceFluxes.map { ChartReferenceLine(log10(it)) },
    )
}
