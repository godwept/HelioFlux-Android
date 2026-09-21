package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.AceEpamSample
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.ChartSeriesStyle
import ca.stewark.helioflux.ui.components.ChartValueFormat
import ca.stewark.helioflux.ui.components.ChartYDomain
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries
import kotlin.math.log10

internal const val EpamMinFlux = 1e-2
internal const val EpamMaxFlux = 1e6
internal val EpamYDomain = ChartYDomain(-2.0, 6.0)

fun exponentialAxisLabel(value: Double): String = "%.1e".format(value)

internal fun epamLogValue(value: Double?): Double? =
    value
        ?.takeIf { it > 0.0 }
        ?.coerceIn(EpamMinFlux, EpamMaxFlux)
        ?.let(::log10)

private fun epamSeries(
    samples: List<AceEpamSample>,
    label: String,
    style: ChartSeriesStyle,
    value: (AceEpamSample) -> Double?,
): LineChartSeries =
    LineChartSeries(
        points =
            samples.map { sample ->
                ChartPoint(sample.timestampMillis.toDouble(), epamLogValue(value(sample)))
            },
        style = style,
        label = label,
        valueFormat = ChartValueFormat.LogScientific,
        allowReduction = true,
    )

internal fun aceEpamSeries(samples: List<AceEpamSample>): List<LineChartSeries> =
    listOf(
        epamSeries(samples, "Proton 47-68 keV", ChartSeriesStyle.Alert) { it.protonLow },
        epamSeries(samples, "Proton 115-195 keV", ChartSeriesStyle.Warning) { it.protonMid },
        epamSeries(samples, "Proton 310-580 keV", ChartSeriesStyle.Primary) { it.protonHigh },
        epamSeries(samples, "Proton 795-1193 keV", ChartSeriesStyle.Secondary) { it.protonFp6 },
        epamSeries(samples, "Proton 1060-1900 keV", ChartSeriesStyle.Success) { it.protonP7 },
    )

@Composable
fun AceEpamChart(
    samples: List<AceEpamSample>,
    domain: ChartDomain,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) {
        Text("ACE EPAM data unavailable", modifier)
        return
    }

    HelioFluxLineChart(
        series = aceEpamSeries(samples),
        domain = domain,
        modifier = modifier.fillMaxWidth().height(240.dp),
        fixedYDomain = EpamYDomain,
        yAxisFormat = ChartValueFormat.LogScientific,
    )
}
