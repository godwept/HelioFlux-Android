package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.SolarImage
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

internal fun solarHeroLoading(frames: List<SolarImage>, preloadComplete: Boolean) =
    !solarHeroCanAnimate(frames) || !preloadComplete

internal fun solarHeroCanAnimate(frames: List<SolarImage>) = frames.map { it.url }.distinct().size > 1

internal fun solarBlendProgress(elapsedMillis: Long, cadenceMillis: Long): Float =
    if (cadenceMillis <= 0L) 1f else (elapsedMillis.toFloat() / cadenceMillis).coerceIn(0f, 1f)

internal fun selectPlayableSolarFrames(frames: List<SolarImage>, preloadedUrls: Set<String>): List<SolarImage> =
    frames.filter { it.url in preloadedUrls }

internal fun solarHeroLoadingText(completedFrames: Int, totalFrames: Int): String? =
    totalFrames.takeIf { it > 0 }?.let { "Loading frame $completedFrames of $it" }

internal suspend fun preloadSolarFrameUrls(
    urls: List<String>,
    load: suspend (String) -> Boolean,
    onProgress: (completedFrames: Int, totalFrames: Int) -> Unit,
): Set<String> =
    coroutineScope {
        if (urls.isEmpty()) return@coroutineScope emptySet()
        var completedFrames = 0
        onProgress(0, urls.size)
        urls.map { url ->
            async {
                val successful = load(url)
                completedFrames += 1
                onProgress(completedFrames, urls.size)
                url.takeIf { successful }
            }
        }.awaitAll().filterNotNull().toSet()
    }

private fun solarFrameRequest(context: android.content.Context, url: String): ImageRequest =
    ImageRequest.Builder(context).data(url).size(512).build()

private suspend fun preloadSolarFrames(
    context: android.content.Context,
    frames: List<SolarImage>,
    onProgress: (completedFrames: Int, totalFrames: Int) -> Unit,
): Set<String> {
    val imageLoader = SingletonImageLoader.get(context)
    return preloadSolarFrameUrls(
        urls = frames.map { it.url },
        load = { url -> imageLoader.execute(solarFrameRequest(context, url)) is SuccessResult },
        onProgress = onProgress,
    )
}

@Composable
fun SolarHero(
    state: RepositoryState<List<SolarImage>>,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    view: HomeSolarViewState = HomeSolarViewState(),
    backgroundMode: Boolean = false,
    onTransform: (Float, Offset, IntSize) -> Unit = { _, _, _ -> },
    onGestureEnd: () -> Unit = {},
    onReset: () -> Unit = {},
) {
    val frames = when (state) {
        is RepositoryState.Available -> state.data
        is RepositoryState.Failure -> state.retainedData.orEmpty()
        else -> emptyList()
    }
    val context = LocalContext.current
    var preloadedUrls by remember(frames) { mutableStateOf<Set<String>>(emptySet()) }
    var preloadComplete by remember(frames) { mutableStateOf(false) }
    var completedFrames by remember(frames) { mutableIntStateOf(0) }

    LaunchedEffect(frames) {
        preloadComplete = false
        completedFrames = 0
        preloadedUrls =
            if (frames.isEmpty()) {
                emptySet()
            } else {
                preloadSolarFrames(context, frames) { completed, _ ->
                    completedFrames = completed
                }
            }
        preloadComplete = true
    }

    val playableFrames = selectPlayableSolarFrames(frames, preloadedUrls)
    val loading = solarHeroLoading(playableFrames, preloadComplete)
    var frameIndex by remember(frames) { mutableIntStateOf(0) }
    var blend by remember(frames) { mutableFloatStateOf(0f) }

    LaunchedEffect(playing, loading, playableFrames) {
        frameIndex = 0
        blend = 0f
        if (playing && !loading && solarHeroCanAnimate(playableFrames)) {
            while (isActive && playing) {
                val started = withFrameNanos { it }
                var elapsed = 0L
                while (isActive && playing && elapsed < 200L) {
                    withFrameNanos { now -> elapsed = (now - started) / 1_000_000L }
                    blend = solarBlendProgress(elapsed, 200L)
                }
                frameIndex = (frameIndex + 1) % playableFrames.size
                blend = 0f
            }
        }
    }

    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        onTransform(zoom, pan, stageSize)
    }
    var wasTransforming by remember { mutableStateOf(false) }
    LaunchedEffect(transform.isTransformInProgress) {
        if (transform.isTransformInProgress) {
            wasTransforming = true
        } else if (wasTransforming) {
            wasTransforming = false
            onGestureEnd()
        }
    }
    val imageTransform = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = view.scale
            scaleY = view.scale
            val pan = view.panPixels(stageSize)
            translationX = pan.x
            translationY = pan.y
        }

    Box(modifier.testTag("solar-hero"), contentAlignment = Alignment.Center) {
        val stageModifier = if (backgroundMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(1f)
        val gestureModifier = if (backgroundMode) Modifier else Modifier
            .transformable(transform)
            .pointerInput(onReset) { detectTapGestures(onDoubleTap = { onReset() }) }
        Box(
            stageModifier
                .clipToBounds()
                .background(Color.Black)
                .testTag("solar-hero-stage")
                .onSizeChanged { stageSize = it }
                .then(gestureModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (!loading && playableFrames.isNotEmpty()) {
                val current = frameIndex.coerceAtMost(playableFrames.lastIndex)
                val next = (current + 1) % playableFrames.size
                AsyncImage(
                    solarFrameRequest(context, playableFrames[current].url),
                    "Animated AIA 304 Sun",
                    imageTransform.testTag("solar-frame-$current"),
                    contentScale = if (backgroundMode) ContentScale.Crop else ContentScale.Fit,
                )
                if (solarHeroCanAnimate(playableFrames)) {
                    AsyncImage(
                        solarFrameRequest(context, playableFrames[next].url),
                        "Animated AIA 304 next frame",
                        imageTransform.graphicsLayer(alpha = blend),
                        contentScale = if (backgroundMode) ContentScale.Crop else ContentScale.Fit,
                    )
                }
            }

            if (loading) {
                Column(
                    Modifier.testTag("solar-hero-loading"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text(solarHeroLoadingText(completedFrames, frames.size) ?: "Loading...")
                }
            }
            if (state is RepositoryState.Failure) {
                Text("Using cached solar imagery", Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
        }
    }
}
