package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.R
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.ForecastSection
import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import kotlin.math.hypot

private val Orbitron = FontFamily(Font(R.font.orbitron_bold, FontWeight.Bold))
internal const val ExpandedHomeHeroWeight = 0.43f
internal const val ExpandedHomeContentWeight = 0.57f

private data class HeroPlacement(
    val frames: RepositoryState<List<SolarImage>>,
    val modifier: Modifier,
    val view: HomeSolarViewState,
    val backgroundMode: Boolean,
    val onTransform: (Float, Offset, IntSize) -> Unit,
    val onGestureEnd: () -> Unit,
    val onReset: () -> Unit,
)

@Composable
private fun Modifier.homeBackgroundGestures(
    enabled: Boolean,
    regions: HomeGestureRegions,
    onTransform: (Float, Offset, IntSize) -> Unit,
    onGestureEnd: () -> Unit,
    onReset: () -> Unit,
): Modifier {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val currentTransform by rememberUpdatedState(onTransform)
    val currentEnd by rememberUpdatedState(onGestureEnd)
    val currentReset by rememberUpdatedState(onReset)
    return this
        .onGloballyPositioned {
            rootOrigin = it.boundsInRoot().topLeft
            viewport = it.size
        }
        .pointerInput(enabled, regions, rootOrigin, viewport) {
            if (!enabled) return@pointerInput
            var lastTapTime = -1L
            var lastTapPosition = Offset.Zero
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var exposed = regions.isExposed(down.position + rootOrigin)
                var transforming = false
                var hadMultiplePointers = false
                var travel = 0f
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } > 1) hadMultiplePointers = true
                    if (event.changes.any {
                            !it.previousPressed && it.pressed &&
                                !regions.isExposed(it.position + rootOrigin)
                        }) exposed = false
                    val pan = event.calculatePan()
                    val zoom = event.calculateZoom()
                    travel += hypot(pan.x, pan.y)
                    if (exposed && !transforming &&
                        (travel > viewConfiguration.touchSlop ||
                            (hadMultiplePointers && kotlin.math.abs(zoom - 1f) > 0.01f))
                    ) {
                        transforming = true
                        lastTapTime = -1L
                    }
                    if (transforming && event.changes.any { it.pressed }) {
                        currentTransform(zoom, pan, viewport)
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (transforming) {
                    currentEnd()
                } else if (exposed && !hadMultiplePointers && travel <= viewConfiguration.touchSlop) {
                    val close = hypot(
                        down.position.x - lastTapPosition.x,
                        down.position.y - lastTapPosition.y,
                    ) < viewConfiguration.touchSlop * 2
                    if (lastTapTime >= 0 && down.uptimeMillis - lastTapTime < 300 && close) {
                        currentReset()
                        lastTapTime = -1L
                    } else {
                        lastTapTime = down.uptimeMillis
                        lastTapPosition = down.position
                    }
                }
            }
        }
}

@Composable
private fun HelioFluxMasthead(onBounds: ((String, Rect?) -> Unit)? = null) {
    val titleStyle = TextStyle(
        fontFamily = Orbitron,
        fontSize = 30.4.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 6.08.sp,
        textAlign = TextAlign.Center,
    )
    Box(
        Modifier.fillMaxWidth().padding(top = 16.dp).testTag("home-masthead"),
        contentAlignment = Alignment.Center,
    ) {
        Box(homeRegionModifier("home-title", onBounds), contentAlignment = Alignment.Center) {
            Text(
                "HELIOFLUX",
                color = Color(0x59FF9500),
                style = titleStyle.copy(shadow = Shadow(Color(0x59FF9500), Offset.Zero, 28f)),
            )
            Text(
                "HELIOFLUX",
                color = Color(0xB3FF9500),
                style = titleStyle.copy(shadow = Shadow(Color(0xB3FF9500), Offset.Zero, 16f)),
            )
            Text(
                "HELIOFLUX",
                color = Color.Transparent,
                style = titleStyle.copy(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFA85200), Color(0xFFFF9F2A), Color(0xFFFFB347)),
                    ),
                ),
            )
        }
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    expanded: Boolean,
    onDestination: (HelioFluxDestination) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    viewStore: HomeSolarViewStore? = null,
) {
    val context = LocalContext.current
    val store = remember(context, viewStore) { viewStore ?: SharedPreferencesHomeSolarViewStore(context) }
    var solarView by remember(store) { mutableStateOf(store.read()) }
    var backgroundMode by remember(store) { mutableStateOf(solarView.isBackground) }
    val regions = remember { HomeGestureRegions() }
    val onBounds: (String, Rect?) -> Unit = remember(regions) {
        { id, bounds -> if (bounds == null) regions.remove(id) else regions.update(id, bounds) }
    }

    val onReset = {
        solarView = HomeSolarViewState()
        backgroundMode = false
        store.save(HomeSolarViewState())
    }
    val onGestureEnd = {
        val settled = solarView.settle()
        solarView = settled
        backgroundMode = settled.isBackground
        store.save(settled)
    }
    val onTransform: (Float, Offset, IntSize) -> Unit = { zoom, pan, viewport ->
        solarView = solarView.transform(zoom, pan, viewport)
    }
    val rootGestures = Modifier.homeBackgroundGestures(
        backgroundMode,
        regions,
        onTransform,
        onGestureEnd,
        onReset,
    )
    val hero = remember {
        movableContentOf<HeroPlacement> { placement ->
            SolarHero(
                state = placement.frames,
                modifier = placement.modifier,
                view = placement.view,
                backgroundMode = placement.backgroundMode,
                onTransform = placement.onTransform,
                onGestureEnd = placement.onGestureEnd,
                onReset = placement.onReset,
            )
        }
    }
    val heroPlacement: (Modifier, Boolean) -> HeroPlacement = { placementModifier, asBackground ->
        HeroPlacement(
            frames = state.frames,
            modifier = placementModifier,
            view = solarView,
            backgroundMode = asBackground,
            onTransform = onTransform,
            onGestureEnd = onGestureEnd,
            onReset = onReset,
        )
    }
    val forecasts = when (val f = state.forecast) {
        is RepositoryState.Available -> f.data
        is RepositoryState.Failure -> f.retainedData.orEmpty()
        else -> emptyList()
    }

    if (expanded) {
        ExpandedHomeLayout(
            state = state,
            forecasts = forecasts,
            onDestination = onDestination,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            backgroundMode = backgroundMode,
            hero = { placementModifier, asBackground -> hero(heroPlacement(placementModifier, asBackground)) },
            onBounds = onBounds,
            rootGestures = rootGestures,
        )
    } else {
        CompactHomeLayout(
            state = state,
            forecasts = forecasts,
            onDestination = onDestination,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            backgroundMode = backgroundMode,
            hero = { placementModifier, asBackground -> hero(heroPlacement(placementModifier, asBackground)) },
            onBounds = onBounds,
            rootGestures = rootGestures,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactHomeLayout(
    state: HomeUiState,
    forecasts: List<ForecastSection>,
    onDestination: (HelioFluxDestination) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    backgroundMode: Boolean,
    hero: @Composable (Modifier, Boolean) -> Unit,
    onBounds: (String, Rect?) -> Unit,
    rootGestures: Modifier,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("home-pull-refresh"),
    ) {
        Box(rootGestures.fillMaxSize().background(Color.Black).testTag("home-screen")) {
            if (backgroundMode) {
                hero(Modifier.fillMaxSize().testTag("solar-hero-background"), true)
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HelioFluxMasthead(onBounds) }
                item {
                    Column(Modifier.fillMaxWidth().testTag("home-compact")) {
                        if (!backgroundMode) hero(Modifier.fillMaxWidth(), false)
                        CurrentConditions(state.conditions, onDestination, Modifier.fillMaxWidth(), onBounds)
                        ForecastCards(
                            sections = forecasts,
                            expandedLayout = false,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            onBounds = onBounds,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedHomeLayout(
    state: HomeUiState,
    forecasts: List<ForecastSection>,
    onDestination: (HelioFluxDestination) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier,
    backgroundMode: Boolean,
    hero: @Composable (Modifier, Boolean) -> Unit,
    onBounds: (String, Rect?) -> Unit,
    rootGestures: Modifier,
) {
    Box(
        modifier =
            modifier
                .then(rootGestures)
                .fillMaxSize()
                .background(Color.Black)
                .testTag("home-screen"),
    ) {
        if (backgroundMode) hero(Modifier.fillMaxSize().testTag("solar-hero-background"), true)
        Column(Modifier.fillMaxSize()) {
            HelioFluxMasthead(onBounds)
            Row(
                Modifier.fillMaxWidth().weight(1f).testTag("home-expanded"),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(ExpandedHomeHeroWeight)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                            .testTag("home-expanded-hero"),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (!backgroundMode) hero(Modifier.fillMaxWidth(), false)
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier =
                        Modifier
                            .weight(ExpandedHomeContentWeight)
                            .fillMaxHeight()
                            .testTag("home-expanded-content"),
                ) {
                    ExpandedHomeRightContent(
                        state = state,
                        forecasts = forecasts,
                        onDestination = onDestination,
                        onBounds = onBounds,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedHomeRightContent(
    state: HomeUiState,
    forecasts: List<ForecastSection>,
    onDestination: (HelioFluxDestination) -> Unit,
    onBounds: (String, Rect?) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 16.dp)
                .testTag("home-expanded-scroll"),
    ) {
        item {
            CurrentConditions(
                state.conditions,
                onDestination,
                Modifier.fillMaxWidth(),
                onBounds,
            )
        }
        item {
            ForecastCards(
                sections = forecasts,
                expandedLayout = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                onBounds = onBounds,
            )
        }
    }
}
