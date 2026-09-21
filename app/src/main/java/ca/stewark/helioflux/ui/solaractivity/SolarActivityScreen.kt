package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.FreshnessIndicator
import ca.stewark.helioflux.ui.components.HelioFluxSectionHeading
import ca.stewark.helioflux.ui.theme.SpaceMuted

internal const val ExpandedSolarImageryWeight = 0.43f
internal const val ExpandedSolarDataWeight = 0.57f

private fun <T> RepositoryState<T>.screenData(): T? =
    when (this) {
        is RepositoryState.Available -> data
        is RepositoryState.Failure -> retainedData
        else -> null
    }

private fun RepositoryState<*>.screenFreshness(): DataFreshness? =
    when (this) {
        is RepositoryState.Available<*> -> freshness
        is RepositoryState.Empty -> freshness
        is RepositoryState.Failure<*> -> freshness
        RepositoryState.Loading -> null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarActivityScreen(
    state: SolarActivityUiState,
    expanded: Boolean,
    onCmeDetails: (CmeEvent) -> Unit = {},
    nowMillis: Long = System.currentTimeMillis(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    var selectedImagery by remember { mutableStateOf(DefaultSolarGalleryItem) }
    var viewer by remember { mutableStateOf<SolarGalleryItem?>(null) }
    val chartDomain = solarActivityChartDomain(nowMillis)
    val regions = state.activeRegions.screenData().orEmpty()
    val freshness =
        listOf(
            state.probabilities,
            state.magnetogram,
            state.xray,
            state.flares,
            state.cmes,
            state.epam,
        ).firstNotNullOfOrNull { it.screenFreshness() }

    if (expanded) {
        ExpandedSolarActivityLayout(
            state = state,
            selectedImagery = selectedImagery,
            onSelectedImagery = { selectedImagery = it },
            onOpenImagery = { viewer = it },
            onCmeDetails = onCmeDetails,
            chartDomain = chartDomain,
            regions = regions,
            freshness = freshness,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        )
    } else {
        CompactSolarActivityLayout(
            state = state,
            selectedImagery = selectedImagery,
            onSelectedImagery = { selectedImagery = it },
            onOpenImagery = { viewer = it },
            onCmeDetails = onCmeDetails,
            chartDomain = chartDomain,
            regions = regions,
            freshness = freshness,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        )
    }

    viewer?.let { item ->
        FullscreenImageryViewer(
            title =
                when (item) {
                    SolarGalleryItem.Hmi -> "HMI Magnetogram"
                    SolarGalleryItem.LascoC2 -> "LASCO C2"
                    SolarGalleryItem.LascoC3 -> "LASCO C3"
                    SolarGalleryItem.Enlil -> "WSA-Enlil"
                },
            onDismiss = { viewer = null },
        ) {
            when (item) {
                SolarGalleryItem.Hmi ->
                    ZoomableSolarImage(
                        title = "HMI Magnetogram",
                        imageUrl = state.magnetogram.screenData()?.url,
                        modifier = Modifier.fillMaxSize(),
                        overlay = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .testTag("fullscreen-active-region-overlay"),
                            ) {
                                ActiveRegionOverlay(regions)
                            }
                        },
                    )
                SolarGalleryItem.LascoC2 ->
                    ZoomableSolarImage(
                        title = "LASCO C2",
                        imageUrl = state.lascoC2.screenData()?.url,
                        modifier = Modifier.fillMaxSize(),
                    )
                SolarGalleryItem.LascoC3 ->
                    ZoomableSolarImage(
                        title = "LASCO C3",
                        imageUrl = state.lascoC3.screenData()?.url,
                        modifier = Modifier.fillMaxSize(),
                    )
                SolarGalleryItem.Enlil ->
                    EnlilAnimation(
                        state = state.enlil,
                        visible = true,
                        modifier = Modifier.fillMaxSize(),
                        testTag = "fullscreen-enlil-player",
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSolarActivityLayout(
    state: SolarActivityUiState,
    selectedImagery: SolarGalleryItem,
    onSelectedImagery: (SolarGalleryItem) -> Unit,
    onOpenImagery: (SolarGalleryItem) -> Unit,
    onCmeDetails: (CmeEvent) -> Unit,
    chartDomain: ChartDomain,
    regions: List<ActiveRegion>,
    freshness: DataFreshness?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("solar-activity-pull-refresh"),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("solar-activity-compact"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            freshness?.let {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FreshnessIndicator(it)
                    }
                }
            }
            item {
                FlareProbabilityBadges(state.probabilities)
            }
            item {
                HelioFluxSectionHeading("Solar Imagery", topSpacing = 0.dp)
            }
            item {
                SolarImageryGallery(
                    selected = selectedImagery,
                    onSelected = onSelectedImagery,
                    magnetogram = state.magnetogram,
                    lascoC2 = state.lascoC2,
                    lascoC3 = state.lascoC3,
                    regions = regions,
                    enlil = state.enlil,
                    onOpen = onOpenImagery,
                )
            }
            solarActivityScienceItems(
                state = state,
                chartDomain = chartDomain,
                onCmeDetails = onCmeDetails,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedSolarActivityLayout(
    state: SolarActivityUiState,
    selectedImagery: SolarGalleryItem,
    onSelectedImagery: (SolarGalleryItem) -> Unit,
    onOpenImagery: (SolarGalleryItem) -> Unit,
    onCmeDetails: (CmeEvent) -> Unit,
    chartDomain: ChartDomain,
    regions: List<ActiveRegion>,
    freshness: DataFreshness?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("solar-activity-expanded"),
    ) {
        Column(
            Modifier
                .weight(ExpandedSolarImageryWeight)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                .testTag("solar-activity-expanded-imagery"),
        ) {
            HelioFluxSectionHeading("Solar Imagery", topSpacing = 0.dp)
            SolarImageryGallery(
                selected = selectedImagery,
                onSelected = onSelectedImagery,
                magnetogram = state.magnetogram,
                lascoC2 = state.lascoC2,
                lascoC3 = state.lascoC3,
                regions = regions,
                enlil = state.enlil,
                onOpen = onOpenImagery,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .weight(ExpandedSolarDataWeight)
                    .fillMaxHeight()
                    .testTag("solar-activity-expanded-content"),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("solar-activity-expanded-scroll"),
                contentPadding =
                    PaddingValues(
                        start = 8.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 12.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                freshness?.let {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            FreshnessIndicator(it)
                        }
                    }
                }
                item {
                    FlareProbabilityBadges(state.probabilities)
                }
                solarActivityScienceItems(
                    state = state,
                    chartDomain = chartDomain,
                    onCmeDetails = onCmeDetails,
                )
            }
        }
    }
}

private fun LazyListScope.solarActivityScienceItems(
    state: SolarActivityUiState,
    chartDomain: ChartDomain,
    onCmeDetails: (CmeEvent) -> Unit,
) {
    item {
        HelioFluxSectionHeading("X-Ray Activity")
    }
    item {
        XraySection(state.xray, chartDomain)
    }
    item {
        HelioFluxSectionHeading("Recent Flares")
    }
    item {
        FlareList(state.flares)
    }
    item {
        HelioFluxSectionHeading("Recent CMEs")
    }
    item {
        CmeList(state.cmes, onCmeDetails)
    }
    item {
        HelioFluxSectionHeading("Particle Environment")
    }
    item {
        ParticleSection(state.epam, chartDomain)
    }
}

@Composable
private fun XraySection(
    state: RepositoryState<List<XrayFluxSample>>,
    chartDomain: ChartDomain,
) {
    val samples = state.screenData().orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            state is RepositoryState.Loading ->
                Text("Loading X-Ray data", color = SpaceMuted)
            state is RepositoryState.Empty ->
                Text("X-Ray data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure && state.retainedData.isNullOrEmpty() ->
                Text("X-Ray data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure ->
                Text(
                    "Showing cached X-Ray data",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceMuted,
                )
        }
        if (samples.isNotEmpty()) {
            XrayChart(samples, chartDomain, Modifier.fillMaxWidth().height(260.dp))
        }
    }
}

@Composable
private fun ParticleSection(
    state: RepositoryState<List<AceEpamSample>>,
    chartDomain: ChartDomain,
) {
    val samples = state.screenData().orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            state is RepositoryState.Loading ->
                Text("Loading particle data", color = SpaceMuted)
            state is RepositoryState.Empty ->
                Text("ACE EPAM data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure && state.retainedData.isNullOrEmpty() ->
                Text("ACE EPAM data unavailable", color = SpaceMuted)
            state is RepositoryState.Failure ->
                Text(
                    "Showing cached particle data",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceMuted,
                )
        }
        if (samples.isNotEmpty()) {
            AceEpamChart(samples, chartDomain)
        }
    }
}
