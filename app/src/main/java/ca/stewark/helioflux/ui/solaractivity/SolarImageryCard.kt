package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface
import coil3.compose.AsyncImage

data class SolarImageryPresentation(
    val title: String,
    val source: String,
    val updatedTime: String?,
    val imageUrl: String?,
)

internal fun solarImageryPresentation(
    title: String,
    source: String,
    state: RepositoryState<SolarImage>,
): SolarImageryPresentation {
    val image =
        when (state) {
            is RepositoryState.Available -> state.data
            is RepositoryState.Failure -> state.retainedData
            else -> null
        }
    return SolarImageryPresentation(
        title = title,
        source = source,
        updatedTime = image?.sourceTimestampMillis?.let(::formatSolarActivityUtc),
        imageUrl = image?.url,
    )
}

@Composable
fun SolarImageryCard(
    title: String,
    source: String,
    state: RepositoryState<SolarImage>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val item = solarImageryPresentation(title, source, state)
    Card(
        modifier = modifier.clickable(enabled = item.imageUrl != null, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = BorderStroke(1.dp, SolarOrange.copy(alpha = 0.24f)),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black),
            ) {
                item.imageUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
                overlay()
                if (state is RepositoryState.Loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                if (item.imageUrl == null && state !is RepositoryState.Loading) {
                    Text(
                        "Image unavailable",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = SpaceMuted,
                    )
                }
            }
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = DataCyan.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, DataCyan.copy(alpha = 0.30f)),
                    ) {
                        Text(
                            item.source,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DataCyan,
                        )
                    }
                }
                item.updatedTime?.let {
                    Text(
                        "Updated " + it,
                        style = MaterialTheme.typography.bodySmall,
                        color = SpaceMuted,
                    )
                }
                if (state is RepositoryState.Failure && state.retainedData != null) {
                    Text(
                        "Showing cached image",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpaceMuted,
                    )
                }
            }
        }
    }
}
