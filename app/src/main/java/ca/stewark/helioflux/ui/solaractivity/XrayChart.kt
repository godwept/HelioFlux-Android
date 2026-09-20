package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.helioflux.core.model.XrayFluxSample
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.ChartReferenceLine
import ca.stewark.helioflux.ui.components.ChartReferenceStyle
import ca.stewark.helioflux.ui.components.ChartSeriesStyle
import ca.stewark.helioflux.ui.components.ChartValueFormat
import ca.stewark.helioflux.ui.components.ChartYDomain
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries
import kotlin.math.log10

const val XrayMinFlux = 1e-9
const val XrayMaxFlux = 1e-2
val XrayReferenceFluxes = listOf(1e-6, 1e-5, 1e-4)

fun xrayLogValue(value: Double?): Double? =
    value?.takeIf { it > 0 }?.coerceIn(XrayMinFlux, XrayMaxFlux)?.let(::log10)

fun xrayReferenceLines(): List<ChartReferenceLine> =
    listOf(
        ChartReferenceLine(
            y = log10(1e-6),
            label = "C",
            style = ChartReferenceStyle.Caution,
        ),
        ChartReferenceLine(
            y = log10(1e-5),
            label = "M",
            style = ChartReferenceStyle.Warning,
        ),
        ChartReferenceLine(
            y = log10(1e-4),
            label = "X",
            style = ChartReferenceStyle.Alert,
        ),
    )

private fun xraySeries(
    samples: List<XrayFluxSample>,
    label: String,
    style: ChartSeriesStyle,
    value: (XrayFluxSample) -> Double?,
): LineChartSeries =
    LineChartSeries(
        points =
            samples.map { sample ->
                ChartPoint(sample.timestampMillis.toDouble(), xrayLogValue(value(sample)))
            },
        style = style,
        label = label,
        valueFormat = ChartValueFormat.XrayFlux,
        allowReduction = true,
    )

@Composable
fun XrayChart(
    samples: List<XrayFluxSample>,
    domain: ChartDomain,
    modifier: Modifier = Modifier,
) {
    HelioFluxLineChart(
        series =
            listOf(
                xraySeries(
                    samples,
                    "GOES-18 Short",
                    ChartSeriesStyle.Secondary,
                ) { it.goes18Short },
                xraySeries(
                    samples,
                    "GOES-18 Long",
                    ChartSeriesStyle.Success,
                ) { it.goes18Long },
                xraySeries(
                    samples,
                    "GOES-19 Short",
                    ChartSeriesStyle.Warning,
                ) { it.goes19Short },
                xraySeries(
                    samples,
                    "GOES-19 Long",
                    ChartSeriesStyle.Alert,
                ) { it.goes19Long },
            ),
        domain = domain,
        modifier = modifier,
        referenceLines = xrayReferenceLines(),
        fixedYDomain = ChartYDomain(-9.0, -2.0),
        yAxisFormat = ChartValueFormat.XrayFlux,
    )
}
