package ca.stewark.helioflux.ui.spaceweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.HemisphericPowerSample
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.ChartSeriesStyle
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries
import ca.stewark.helioflux.ui.components.seriesColor

val hemisphericPowerMeta =
    ChartCardMeta(
        "OVATION Model",
        "Hemispheric Power (GW)",
        listOf("North", "South"),
    )
const val hemisphericPowerEmptyMessage = "No hemispheric power data"
fun hemisphericPowerSeries(
    samples: List<HemisphericPowerSample>,
): List<LineChartSeries> =
    samples.let { filtered ->
        listOf(
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.north) },
                style = ChartSeriesStyle.Secondary,
                label = "North",
                allowReduction = false,
            ),
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.south) },
                style = ChartSeriesStyle.Purple,
                label = "South",
                allowReduction = false,
            ),
        )
    }

@Composable
fun HemisphericPowerChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
            ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                hemisphericPowerMeta.context.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                hemisphericPowerMeta.title,
                style = MaterialTheme.typography.titleMedium,
            )
            LegendRow(
                hemisphericPowerMeta.legendLabels,
                series.map { seriesColor(it.style) },
            )
            if (series.all { item -> item.points.none { point -> point.y != null } }) {
                Text(hemisphericPowerEmptyMessage)
            } else {
                HelioFluxLineChart(
                    series = series,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            }
        }
    }
}
