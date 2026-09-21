package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.EnlilFrame
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

internal fun enlilBlendProgress(
    elapsedMillis: Long,
    cadenceMillis: Long,
): Float =
    if (cadenceMillis <= 0L) {
        1f
    } else {
        (elapsedMillis.toFloat() / cadenceMillis).coerceIn(0f, 1f)
    }

internal fun selectPlayableEnlilFrames(
    urls: List<String>,
    preloadedUrls: Set<String>,
): List<String> {
    val loaded = urls.filter { it in preloadedUrls }
    return loaded.ifEmpty { urls.take(1) }
}

private fun RepositoryState<List<EnlilFrame>>.enlilFrames(): List<EnlilFrame> =
    when (this) {
        is RepositoryState.Available -> data
        is RepositoryState.Failure -> retainedData.orEmpty()
        else -> emptyList()
    }

private suspend fun preloadEnlilFrames(
    context: android.content.Context,
    urls: List<String>,
): Set<String> =
    coroutineScope {
        val loader = SingletonImageLoader.get(context)
        urls
            .map { url ->
                async {
                    url.takeIf {
                        loader.execute(
                            ImageRequest.Builder(context).data(url).build(),
                        ) is SuccessResult
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
            .toSet()
    }

@Composable
fun EnlilAnimation(
    state: RepositoryState<List<EnlilFrame>>,
    visible: Boolean,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val frames = state.enlilFrames()
    val urls = frames.map { it.url }
    val context = LocalContext.current
    var preloaded by remember(urls) { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(urls) {
        preloaded = if (urls.size > 1) preloadEnlilFrames(context, urls) else urls.toSet()
    }
    val playable = selectPlayableEnlilFrames(urls, preloaded)
    var index by remember(playable) { mutableIntStateOf(0) }
    var blend by remember(playable) { mutableFloatStateOf(0f) }
    LaunchedEffect(playable, visible, preloaded) {
        if (visible && playable.size > 1 && preloaded.isNotEmpty()) {
            while (isActive && visible) {
                val started = withFrameNanos { it }
                var elapsed = 0L
                while (isActive && visible && elapsed < 200L) {
                    withFrameNanos { now -> elapsed = (now - started) / 1_000_000L }
                    blend = enlilBlendProgress(elapsed, 200L)
                }
                index = (index + 1) % playable.size
                blend = 0f
            }
        }
    }

    val taggedModifier = if (testTag == null) modifier else modifier.testTag(testTag)
    Box(taggedModifier) {
        if (playable.isNotEmpty()) {
            val current = index.coerceAtMost(playable.lastIndex)
            val next = (current + 1) % playable.size
            AsyncImage(
                model = playable[current],
                contentDescription = "WSA-Enlil frame",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            if (playable.size > 1) {
                AsyncImage(
                    model = playable[next],
                    contentDescription = "WSA-Enlil next frame",
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = blend),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            Text("ENLIL frames unavailable", Modifier.padding(16.dp), color = SpaceMuted)
        }
    }
}

@Composable
fun EnlilCard(
    state: RepositoryState<List<EnlilFrame>>,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frames = state.enlilFrames()
    Card(
        modifier = modifier.clickable(enabled = frames.isNotEmpty(), onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = BorderStroke(1.dp, SolarOrange.copy(alpha = 0.24f)),
    ) {
        Column {
            EnlilAnimation(
                state = state,
                visible = visible,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("WSA-Enlil", style = MaterialTheme.typography.titleMedium)
                    Text("NOAA", style = MaterialTheme.typography.labelSmall, color = DataCyan)
                }
                frames.firstOrNull()?.runTimestampMillis?.let {
                    Text(
                        "Run " + formatSolarActivityUtc(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = SpaceMuted,
                    )
                }
                if (state is RepositoryState.Failure && state.retainedData != null) {
                    Text(
                        "Showing cached ENLIL data",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpaceMuted,
                    )
                }
            }
        }
    }
}
