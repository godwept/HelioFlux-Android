package ca.stewark.helioflux.ui.spaceweather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.core.data.repository.GoesMagnetometerSeries
import ca.stewark.helioflux.core.model.SolarWindMag
import ca.stewark.helioflux.core.model.SolarWindPlasma
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.ChartPoint
import ca.stewark.helioflux.ui.components.ChartReferenceLine
import ca.stewark.helioflux.ui.components.ChartSeriesStyle
import ca.stewark.helioflux.ui.components.HelioFluxLineChart
import ca.stewark.helioflux.ui.components.LineChartSeries
import ca.stewark.helioflux.ui.components.chartDomain
import ca.stewark.helioflux.ui.components.seriesColor
import ca.stewark.helioflux.ui.theme.DataCyan

data class ChartCardMeta(
    val context: String,
    val title: String,
    val legendLabels: List<String> = emptyList(),
)

val bzBtChartMeta =
    ChartCardMeta(
        context = "Magnetic Field",
        title = "IMF Bz / Bt (nT)",
        legendLabels = listOf("Bz", "Bt"),
    )
val densityChartMeta = ChartCardMeta("Solar Wind", "Density (p/cm³)")
val speedChartMeta = ChartCardMeta("Solar Wind", "Speed (km/s)")
val temperatureChartMeta = ChartCardMeta("Solar Wind", "Temperature (K)")
val SpaceWeatherChartHeight = 240.dp

fun goesChartMeta(primary: String?, secondary: String?) =
    ChartCardMeta(
        context = "GOES Magnetometer",
        title = "Magnetic Field (nT)",
        legendLabels = listOfNotNull(primary, secondary),
    )

fun spaceWeatherChartDomain(timeframe: Timeframe, now: Long): ChartDomain =
    chartDomain(now, timeframe.durationMillis)

fun bzBtSeries(
    samples: List<SolarWindMag>,
    timeframe: Timeframe,
    now: Long,
): List<LineChartSeries> =
    filterByTimeframe(samples, timeframe, now) { it.timestampMillis }.let { filtered ->
        listOf(
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.bz) },
                style = ChartSeriesStyle.Bz,
                label = "Bz",
            ),
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.bt) },
                style = ChartSeriesStyle.Secondary,
                label = "Bt",
            ),
        )
    }

fun plasmaSeries(
    samples: List<SolarWindPlasma>,
    timeframe: Timeframe,
    now: Long,
    label: String = "",
    style: ChartSeriesStyle = ChartSeriesStyle.Default,
    field: (SolarWindPlasma) -> Double?,
): List<LineChartSeries> =
    listOf(
        LineChartSeries(
            points =
                filterByTimeframe(samples, timeframe, now) { it.timestampMillis }
                    .map { ChartPoint(it.timestampMillis.toDouble(), field(it)) },
            style = style,
            label = label,
        ),
    )

fun goesSeries(
    series: GoesMagnetometerSeries,
    timeframe: Timeframe,
    now: Long,
): List<LineChartSeries> =
    filterByTimeframe(series.samples, timeframe, now) { it.timestampMillis }.let { filtered ->
        listOf(
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.primary) },
                style = ChartSeriesStyle.Primary,
                label = series.primaryLabel ?: "Primary",
            ),
            LineChartSeries(
                points = filtered.map { ChartPoint(it.timestampMillis.toDouble(), it.secondary) },
                style = ChartSeriesStyle.Alert,
                label = series.secondaryLabel ?: "Secondary",
            ),
        )
    }

fun goesSeriesOrEmpty(
    series: GoesMagnetometerSeries?,
    timeframe: Timeframe,
    now: Long,
): List<LineChartSeries> =
    series?.let { goesSeries(it, timeframe, now) }.orEmpty()

@Composable
fun SpaceWeatherLineCard(
    meta: ChartCardMeta,
    series: List<LineChartSeries>,
    domain: ChartDomain,
    modifier: Modifier = Modifier,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    onRefresh: (() -> Unit)? = null,
    refreshing: Boolean = false,
    refreshContentDescription: String = "Refresh chart",
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        meta.context.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(meta.title, style = MaterialTheme.typography.titleMedium)
                }
                onRefresh?.let {
                    ChartRefreshAction(
                        contentDescription = refreshContentDescription,
                        refreshing = refreshing,
                        onRefresh = it,
                    )
                }
            }
            if (meta.legendLabels.isNotEmpty()) {
                LegendRow(meta.legendLabels, series.map { seriesColor(it.style) })
            }
            HelioFluxLineChart(
                series = series,
                modifier = Modifier.fillMaxWidth().height(SpaceWeatherChartHeight),
                referenceLines = referenceLines,
                domain = domain,
            )
        }
    }
}

@Composable
fun LegendRow(labels: List<String>, colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.forEachIndexed { index, label ->
            Text(
                "● $label",
                style = MaterialTheme.typography.labelSmall,
                color = colors.getOrElse(index) { DataCyan },
            )
        }
    }
}

@Composable
internal fun ChartRefreshAction(
    contentDescription: String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    IconButton(
        onClick = onRefresh,
        enabled = !refreshing,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    ) {
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "↻",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
