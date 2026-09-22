package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.stewark.helioflux.ui.theme.SpaceMuted
import coil3.compose.AsyncImage

class ImageryTransformState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    fun transform(
        zoomChange: Float,
        panChange: Offset,
    ) {
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + panChange
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}

@Composable
fun FullscreenImageryViewer(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen-imagery-viewer"),
        ) {
            Box(Modifier.fillMaxSize()) {
                content()
            }
            Text(
                title,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = SpaceMuted,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).testTag("fullscreen-imagery-close"),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ZoomableSolarImage(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    trackMediaLoading: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val transform = remember(imageUrl) { ImageryTransformState() }
    val gesture =
        rememberTransformableState { zoom, pan, _ ->
            transform.transform(zoom, pan)
        }
    var mediaState by remember(imageUrl, trackMediaLoading) {
        mutableStateOf(
            if (trackMediaLoading) {
                initialSolarMediaLoadState(imageUrl)
            } else {
                SolarMediaLoadState.Ready
            },
        )
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val stageSize = minOf(maxWidth, maxHeight)
        Box(
            Modifier
                .size(stageSize)
                .graphicsLayer {
                    scaleX = transform.scale
                    scaleY = transform.scale
                    translationX = transform.offset.x
                    translationY = transform.offset.y
                }
                .transformable(gesture),
        ) {
            if (imageUrl != null) {
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
                            mediaState =
                                reduceSolarMediaLoadState(SolarMediaLoadEvent.Success)
                        }
                    },
                    onError = {
                        if (trackMediaLoading) {
                            mediaState =
                                reduceSolarMediaLoadState(SolarMediaLoadEvent.Error)
                        }
                    },
                )
            }

            if (
                trackMediaLoading &&
                imageUrl != null &&
                mediaState == SolarMediaLoadState.Loading
            ) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .testTag("fullscreen-solar-media-loading"),
                )
            }

            if (
                imageUrl == null ||
                (
                    trackMediaLoading &&
                        mediaState == SolarMediaLoadState.Failed
                )
            ) {
                Text(
                    "Image unavailable",
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .testTag("fullscreen-solar-media-unavailable"),
                    color = SpaceMuted,
                )
            }
            overlay()
        }
    }
}
