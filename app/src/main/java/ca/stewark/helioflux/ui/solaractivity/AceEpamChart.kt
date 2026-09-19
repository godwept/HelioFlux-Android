package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.AceEpamSample
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries

fun exponentialAxisLabel(value: Double): String = "%.1e".format(value)

private fun epamSeries(
    samples: List<AceEpamSample>,
    value: (AceEpamSample) -> Double?,
): LineChartSeries = LineChartSeries(
    samples.map { sample -> ChartPoint(sample.timestampMillis.toDouble(), value(sample)) },
)

@Composable
fun AceEpamChart(samples: List<AceEpamSample>, modifier: Modifier = Modifier) {
    if (samples.isEmpty()) {
        Text("ACE EPAM data unavailable", modifier)
        return
    }
    Column(modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Text("ACE EPAM")
        HelioFluxLineChart(
            listOf(
                epamSeries(samples) { it.electronLow },
                epamSeries(samples) { it.protonLow },
                epamSeries(samples) { it.protonMid },
            ),
            Modifier.fillMaxWidth().height(240.dp),
        )
    }
}
