package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal const val EnlilPreloadConcurrency = 4
internal const val EnlilMaxToleratedFailures = 5

internal data class EnlilPreloadResult(
    val successfulUrls: List<String>,
    val failureCount: Int,
)

internal enum class EnlilMediaLoadState {
    Loading,
    Ready,
    Failed,
}

internal fun enlilBlendProgress(
    elapsedMillis: Long,
    cadenceMillis: Long,
): Float =
    if (cadenceMillis <= 0L) {
        1f
    } else {
        (elapsedMillis.toFloat() / cadenceMillis).coerceIn(0f, 1f)
    }

internal fun isAcceptedEnlilPreload(result: EnlilPreloadResult): Boolean =
    result.successfulUrls.isNotEmpty() &&
        result.failureCount <= EnlilMaxToleratedFailures

internal fun playableEnlilFrames(result: EnlilPreloadResult?): List<String> =
    result
        ?.takeIf(::isAcceptedEnlilPreload)
        ?.successfulUrls
        .orEmpty()

internal fun enlilMediaLoadState(
    urls: List<String>,
    preloadResult: EnlilPreloadResult?,
): EnlilMediaLoadState =
    when {
        urls.isEmpty() -> EnlilMediaLoadState.Failed
        preloadResult == null -> EnlilMediaLoadState.Loading
        isAcceptedEnlilPreload(preloadResult) -> EnlilMediaLoadState.Ready
        else -> EnlilMediaLoadState.Failed
    }

internal suspend fun preloadEnlilUrls(
    urls: List<String>,
    maxConcurrency: Int = EnlilPreloadConcurrency,
    load: suspend (String) -> Boolean,
): EnlilPreloadResult =
    coroutineScope {
        require(maxConcurrency > 0)
        val semaphore = Semaphore(maxConcurrency)
        val results =
            urls.map { url ->
                async {
                    val loaded =
                        semaphore.withPermit {
                            try {
                                load(url)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                false
                            }
                        }
                    url to loaded
                }
            }.awaitAll()

        EnlilPreloadResult(
            successfulUrls = results.filter { it.second }.map { it.first },
            failureCount = results.count { !it.second },
        )
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
): EnlilPreloadResult {
    val loader = SingletonImageLoader.get(context)
    return preloadEnlilUrls(urls) { url ->
        loader.execute(
            ImageRequest.Builder(context).data(url).build(),
        ) is SuccessResult
    }
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
    var preloadResult by remember(urls) { mutableStateOf<EnlilPreloadResult?>(null) }

    LaunchedEffect(urls) {
        preloadResult = null
        if (urls.isNotEmpty()) {
            preloadResult = preloadEnlilFrames(context, urls)
        }
    }

    val mediaState =
        if (state is RepositoryState.Loading) {
            EnlilMediaLoadState.Loading
        } else {
            enlilMediaLoadState(urls, preloadResult)
        }
    val playable =
        if (mediaState == EnlilMediaLoadState.Ready) {
            playableEnlilFrames(preloadResult)
        } else {
            emptyList()
        }

    var index by remember(playable) { mutableIntStateOf(0) }
    var blend by remember(playable) { mutableFloatStateOf(0f) }
    LaunchedEffect(playable, visible, mediaState) {
        index = 0
        blend = 0f
        if (
            mediaState == EnlilMediaLoadState.Ready &&
            visible &&
            playable.size > 1
        ) {
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
    Box(
        modifier = taggedModifier,
        contentAlignment = Alignment.Center,
    ) {
        when (mediaState) {
            EnlilMediaLoadState.Loading ->
                CircularProgressIndicator(
                    Modifier.testTag("enlil-loading"),
                )
            EnlilMediaLoadState.Failed ->
                Text(
                    "ENLIL frames unavailable",
                    modifier = Modifier.padding(16.dp).testTag("enlil-unavailable"),
                    color = SpaceMuted,
                )
            EnlilMediaLoadState.Ready -> {
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
            }
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
