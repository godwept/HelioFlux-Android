package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.CmeEvent
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted

enum class CmeSpeedEmphasis {
    Normal,
    Elevated,
    Strong,
}

fun cmeSpeedEmphasis(speed: Double?): CmeSpeedEmphasis =
    when {
        speed == null || speed < 500 -> CmeSpeedEmphasis.Normal
        speed < 1000 -> CmeSpeedEmphasis.Elevated
        else -> CmeSpeedEmphasis.Strong
    }

@Composable
fun CmeList(
    state: RepositoryState<List<CmeEvent>>,
    onDetails: (CmeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val events =
        when (state) {
            is RepositoryState.Available -> state.data
            is RepositoryState.Failure -> state.retainedData.orEmpty()
            else -> emptyList()
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            state is RepositoryState.Loading ->
                Text("Loading CME data", color = SpaceMuted)
            state is RepositoryState.Empty ->
                Text("No recent CMEs", color = SpaceMuted)
            state is RepositoryState.Failure && state.retainedData == null ->
                Text("CME data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure ->
                Text(
                    "Showing cached CME data",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceMuted,
                )
            events.isEmpty() ->
                Text("No recent CMEs", color = SpaceMuted)
        }

        events.forEachIndexed { index, cme ->
            val item = cmeEventPresentation(cme)
            val accent =
                when (item.emphasis) {
                    CmeSpeedEmphasis.Normal -> SpaceMuted
                    CmeSpeedEmphasis.Elevated -> SolarOrange
                    CmeSpeedEmphasis.Strong -> AlertRed
                }
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("cme-row-${cme.id}")
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        item.speed,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        item.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = SpaceMuted,
                    )
                }
                if (item.metadata.isNotBlank()) {
                    Text(
                        item.metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = SpaceMuted,
                    )
                }
                if (cme.link != null) {
                    TextButton(onClick = { onDetails(cme) }) {
                        Text("Details")
                    }
                }
            }
            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
