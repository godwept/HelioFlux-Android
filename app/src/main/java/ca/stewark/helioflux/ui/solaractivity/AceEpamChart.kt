package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries

fun exponentialAxisLabel(value: Double): String = "%.1e".format(value)

private fun epamSeries(
    samples: List<AceEpamSample>,
    label: String,
    style: ChartSeriesStyle,
    value: (AceEpamSample) -> Double?,
): LineChartSeries =
    LineChartSeries(
        points =
            samples.map { sample ->
                ChartPoint(sample.timestampMillis.toDouble(), value(sample))
            },
        style = style,
        label = label,
        valueFormat = ChartValueFormat.Scientific,
        allowReduction = true,
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

    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ACE EPAM")
        HelioFluxLineChart(
            series =
                listOf(
                    epamSeries(
                        samples,
                        "Electron 38-53 keV",
                        ChartSeriesStyle.Secondary,
                    ) { it.electronLow },
                    epamSeries(
                        samples,
                        "Proton 47-68 keV",
                        ChartSeriesStyle.Warning,
                    ) { it.protonLow },
                    epamSeries(
                        samples,
                        "Proton 115-195 keV",
                        ChartSeriesStyle.Alert,
                    ) { it.protonMid },
                ),
            domain = domain,
            modifier = Modifier.fillMaxWidth().height(240.dp),
            yAxisFormat = ChartValueFormat.Scientific,
        )
    }
}
