package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

internal enum class SolarHeroPhase { InitialLoading, PosterLoading, Playing, StaticPoster }

internal fun solarHeroPhase(
    frames: List<SolarImage>,
    preloadedUrls: Set<String>,
    preloadComplete: Boolean,
): SolarHeroPhase = when {
    frames.isEmpty() -> SolarHeroPhase.InitialLoading
    frames.size == 1 -> SolarHeroPhase.StaticPoster
    !preloadComplete -> SolarHeroPhase.PosterLoading
    frames.count { it.url in preloadedUrls } > 1 -> SolarHeroPhase.Playing
    else -> SolarHeroPhase.StaticPoster
}

internal fun selectSolarPoster(frames: List<SolarImage>): SolarImage? = frames.firstOrNull()

internal fun frameIdentityFor(frames: List<SolarImage>): List<String> = frames.map { it.url }

internal fun shouldShowSolarSpinner(
    phase: SolarHeroPhase,
    posterLoaded: Boolean,
    playbackLayerReady: Boolean,
): Boolean = phase == SolarHeroPhase.InitialLoading ||
    phase == SolarHeroPhase.PosterLoading ||
    (phase == SolarHeroPhase.Playing && !playbackLayerReady) ||
    (phase == SolarHeroPhase.StaticPoster && !posterLoaded)

internal fun solarBlendProgress(elapsedMillis: Long, cadenceMillis: Long): Float =
    if (cadenceMillis <= 0L) 1f else (elapsedMillis.toFloat() / cadenceMillis).coerceIn(0f, 1f)

internal fun selectPlayableSolarFrames(frames: List<SolarImage>, preloadedUrls: Set<String>): List<SolarImage> {
    if (frames.isEmpty()) return emptyList()
    val preloaded = frames.filter { it.url in preloadedUrls }
    return preloaded.ifEmpty { listOf(frames.first()) }
}

private suspend fun preloadSolarFrames(context: android.content.Context, frames: List<SolarImage>): Set<String> = coroutineScope {
    val imageLoader = SingletonImageLoader.get(context)
    frames.map { frame ->
        async {
            val request = ImageRequest.Builder(context).data(frame.url).size(512).build()
            frame.url.takeIf { imageLoader.execute(request) is SuccessResult }
        }
    }.awaitAll().filterNotNull().toSet()
}

@Composable
fun SolarHero(
    state: RepositoryState<List<SolarImage>>,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
) {
    val frames = when (state) {
        is RepositoryState.Available -> state.data
        is RepositoryState.Failure -> state.retainedData.orEmpty()
        else -> emptyList()
    }
    val context = LocalContext.current
    var preloadedUrls by remember(frames) { mutableStateOf<Set<String>>(emptySet()) }
    var preloadComplete by remember(frames) { mutableStateOf(frames.size <= 1) }

    LaunchedEffect(frames) {
        preloadedUrls = emptySet()
        preloadComplete = frames.size <= 1
        if (frames.isNotEmpty()) {
            preloadedUrls = if (frames.size > 1) preloadSolarFrames(context, frames) else setOf(frames.first().url)
            preloadComplete = true
        }
    }

    val phase = solarHeroPhase(frames, preloadedUrls, preloadComplete)
    val playableFrames = selectPlayableSolarFrames(frames, preloadedUrls)
    val frameIdentity = frameIdentityFor(frames)
    // Readiness is visual, not just cache/preload completion.
    var posterLoaded by remember(frameIdentity) { mutableStateOf(false) }
    var playbackLayerReady by remember(frameIdentity) { mutableStateOf(false) }
    var frameIndex by remember(frameIdentity) { mutableIntStateOf(0) }
    var blend by remember(frameIdentity) { mutableFloatStateOf(0f) }

    LaunchedEffect(playing, phase, frameIdentity) {
        if (playing && phase == SolarHeroPhase.Playing && playableFrames.size > 1) {
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

    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val offset = Offset(offsetX, offsetY)
    val transform = rememberTransformableState { zoom, pan, _ ->
        val next = (scale * zoom).coerceIn(1f, 4f)
        scale = next
        val nextOffset = if (next == 1f) Offset.Zero else offset + pan
        offsetX = nextOffset.x
        offsetY = nextOffset.y
    }
    val imageTransform = Modifier
        .fillMaxSize()
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }

    Box(modifier.testTag("solar-hero"), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
                .testTag("solar-hero-stage")
                .transformable(transform)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            // Keep the poster mounted beneath playback. Removing it at the phase
            // transition can expose a blank frame while Coil attaches cached images.
            selectSolarPoster(frames)?.let {
                AsyncImage(
                    it.url,
                    "AIA 304 Sun",
                    imageTransform.testTag("solar-poster"),
                    onSuccess = { posterLoaded = true },
                )
            }

            if (phase == SolarHeroPhase.Playing) {
                val current = frameIndex.coerceAtMost(playableFrames.lastIndex)
                val next = (current + 1) % playableFrames.size
                AsyncImage(
                    playableFrames[current].url,
                    "Animated AIA 304 Sun",
                    imageTransform.testTag("solar-frame-" + current),
                    onSuccess = { playbackLayerReady = true },
                )
                AsyncImage(
                    playableFrames[next].url,
                    "Animated AIA 304 next frame",
                    imageTransform.graphicsLayer(alpha = blend),
                )
            }

            val showSpinner = shouldShowSolarSpinner(phase, posterLoaded, playbackLayerReady)
            if (showSpinner) {
                CircularProgressIndicator(Modifier.testTag("solar-hero-loading"))
            }
            if (state is RepositoryState.Failure) {
                Text("Using cached solar imagery", Modifier.align(Alignment.BottomCenter).padding(12.dp))
            }
        }
    }
}
