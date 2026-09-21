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
import ca.stewark.helioflux.core.model.KpSample
import ca.stewark.helioflux.core.model.KpStatus
import ca.stewark.helioflux.core.model.kpStatus
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.ChartYDomain
import ca.stewark.helioflux.ui.components.chartDomain
import ca.stewark.helioflux.ui.components.HelioFluxBarChart

data class KpPresentation(
    val values: List<Pair<Double, Double>>,
    val currentStatus: KpStatus?,
)

val kpChartMeta = ChartCardMeta("Geomagnetic Activity", "Planetary Kp Index (3-hour)")
val KpYDomain = ChartYDomain(0.0, 9.0)

fun kpStatusLabel(status: KpStatus?) = status?.name ?: "No current Kp"

fun kpPresentation(
    samples: List<KpSample>,
    now: Long,
): KpPresentation {
    val available =
        samples.mapNotNull { sample ->
            sample.kp?.let { value -> sample.timestampMillis.toDouble() to value }
        }
    val visible = available.filter { it.first >= now - Timeframe.TwoDays.durationMillis && it.first <= now }
    return KpPresentation(
        values = visible,
        currentStatus = available.lastOrNull()?.second?.let(::kpStatus),
    )
}

fun kpChartDomain(now: Long): ChartDomain =
    chartDomain(now, Timeframe.TwoDays.durationMillis)

@Composable
fun KpChart(
    presentation: KpPresentation,
    domain: ChartDomain,
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
                kpChartMeta.context.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(kpChartMeta.title, style = MaterialTheme.typography.titleMedium)
            Text(
                kpStatusLabel(presentation.currentStatus),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            HelioFluxBarChart(
                values = presentation.values,
                domain = domain,
                fixedYDomain = KpYDomain,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }
    }
}
