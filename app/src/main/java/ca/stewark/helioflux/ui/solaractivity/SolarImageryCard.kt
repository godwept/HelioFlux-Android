package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.imageloading.ImageDownloadProgress
import ca.stewark.helioflux.imageloading.imageDownloadProgressRegistry
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface
import coil3.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

enum class SolarMediaLoadState {
    Loading,
    Ready,
    Failed,
}

enum class SolarMediaLoadEvent {
    Success,
    Error,
}

internal fun initialSolarMediaLoadState(imageUrl: String?): SolarMediaLoadState =
    if (imageUrl == null) SolarMediaLoadState.Failed else SolarMediaLoadState.Loading

internal fun reduceSolarMediaLoadState(event: SolarMediaLoadEvent): SolarMediaLoadState =
    when (event) {
        SolarMediaLoadEvent.Success -> SolarMediaLoadState.Ready
        SolarMediaLoadEvent.Error -> SolarMediaLoadState.Failed
    }

internal fun shouldShowSolarMediaSpinner(
    repositoryLoading: Boolean,
    imageUrl: String?,
    mediaState: SolarMediaLoadState,
): Boolean =
    repositoryLoading ||
        (imageUrl != null && mediaState == SolarMediaLoadState.Loading)

internal fun formatSolarDownloadProgress(progress: ImageDownloadProgress): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    val downloadedKilobytes = progress.bytesRead / 1024L
    val totalKilobytes = progress.totalBytes?.div(1024L)
    return if (totalKilobytes == null) {
        "Downloading… " + formatter.format(downloadedKilobytes) + " KB"
    } else {
        "Downloading… " +
            formatter.format(downloadedKilobytes) +
            " KB / " +
            formatter.format(totalKilobytes) +
            " KB"
    }
}

@Composable
internal fun rememberSolarDownloadProgress(
    imageUrl: String?,
    enabled: Boolean,
): ImageDownloadProgress? {
    val progressFlow =
        remember(imageUrl, enabled) {
            if (enabled && imageUrl != null) {
                imageDownloadProgressRegistry.observe(imageUrl)
            } else {
                flowOf<ImageDownloadProgress?>(null)
            }
        }
    return progressFlow.collectAsState(initial = null).value
}

@Composable
internal fun SolarMediaLoadingIndicator(
    progressText: String?,
    loadingTag: String,
    progressTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(loadingTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator()
        progressText?.let {
            Text(
                it,
                modifier = Modifier.testTag(progressTag),
                style = MaterialTheme.typography.labelMedium,
                color = SpaceMuted,
            )
        }
    }
}

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
    trackMediaLoading: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val item = solarImageryPresentation(title, source, state)
    var mediaState by remember(item.imageUrl, trackMediaLoading) {
        mutableStateOf(
            if (trackMediaLoading) {
                initialSolarMediaLoadState(item.imageUrl)
            } else {
                SolarMediaLoadState.Ready
            },
        )
    }
    val downloadProgress = rememberSolarDownloadProgress(item.imageUrl, trackMediaLoading)
    val showSpinner =
        shouldShowSolarMediaSpinner(
            repositoryLoading = state is RepositoryState.Loading,
            imageUrl = item.imageUrl,
            mediaState = mediaState,
        )
    val mediaFailed =
        trackMediaLoading &&
            item.imageUrl != null &&
            mediaState == SolarMediaLoadState.Failed
    val progressText =
        downloadProgress
            ?.takeIf { mediaState == SolarMediaLoadState.Loading }
            ?.let(::formatSolarDownloadProgress)

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
                item.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onLoading = {
                            if (trackMediaLoading) {
                                mediaState = SolarMediaLoadState.Loading
                            }
                        },
                        onSuccess = {
                            if (trackMediaLoading) {
                                imageDownloadProgressRegistry.clear(imageUrl)
                                mediaState =
                                    reduceSolarMediaLoadState(SolarMediaLoadEvent.Success)
                            }
                        },
                        onError = {
                            if (trackMediaLoading) {
                                imageDownloadProgressRegistry.clear(imageUrl)
                                mediaState =
                                    reduceSolarMediaLoadState(SolarMediaLoadEvent.Error)
                            }
                        },
                    )
                }
                overlay()
                if (showSpinner) {
                    SolarMediaLoadingIndicator(
                        progressText = progressText,
                        loadingTag = "solar-media-loading",
                        progressTag = "solar-media-download-progress",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (
                    (item.imageUrl == null && state !is RepositoryState.Loading) ||
                        mediaFailed
                ) {
                    Text(
                        "Image unavailable",
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                                .testTag("solar-media-unavailable"),
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
