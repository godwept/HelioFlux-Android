package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.FlareEvent
import ca.stewark.helioflux.ui.theme.SpaceMuted

fun flareClassGroup(value: String): Char =
    value.firstOrNull()?.uppercaseChar()?.takeIf { it in "ABCMX" } ?: 'A'

@Composable
fun FlareList(
    state: RepositoryState<List<FlareEvent>>,
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
                Text("Loading flare data", color = SpaceMuted)
            state is RepositoryState.Empty ->
                Text("No recent flares", color = SpaceMuted)
            state is RepositoryState.Failure && state.retainedData == null ->
                Text("Flare data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure ->
                Text(
                    "Showing cached flare data",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceMuted,
                )
            events.isEmpty() ->
                Text("No recent flares", color = SpaceMuted)
        }

        events.forEachIndexed { index, flare ->
            val item = flareEventPresentation(flare)
            Column(
                Modifier
                    .fillMaxWidth()
                    .testTag("flare-row-${flare.id}")
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        item.flareClass,
                        style = MaterialTheme.typography.titleMedium,
                        color = item.accent,
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
            }
            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
