package ca.stewark.helioflux.ui.spaceweather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.feature.globe.AuroraGlobe
import ca.stewark.helioflux.ui.components.*

private fun <T> RepositoryState<T>.screenData(): T? =
    when (this) {
        is RepositoryState.Available -> data
        is RepositoryState.Failure -> retainedData
        else -> null
    }

data class AuroraGlobePresentation(
    val points: List<AuroraPoint>,
    val freshness: DataFreshness?,
)

fun auroraGlobePresentation(state: RepositoryState<AuroraSnapshot>): AuroraGlobePresentation {
    val snapshot = state.screenData()
    val freshness =
        when (state) {
            is RepositoryState.Available -> state.freshness
            is RepositoryState.Empty -> state.freshness
            is RepositoryState.Failure -> state.freshness
            RepositoryState.Loading -> null
        }
    return AuroraGlobePresentation(snapshot?.points.orEmpty(), freshness)
}

enum class SpaceWeatherBlock {
    AuroraHero,
    SolarWindHeading,
    Metrics,
    Timeframe,
    BzBt,
    Density,
    Speed,
    Temperature,
    Goes,
    GeomagneticHeading,
    Kp,
    HemisphericPower,
}

fun refreshSourceForBlock(block: SpaceWeatherBlock): SpaceWeatherRefreshSource? =
    when (block) {
        SpaceWeatherBlock.BzBt -> SpaceWeatherRefreshSource.Magnetic
        SpaceWeatherBlock.Density,
        SpaceWeatherBlock.Speed,
        SpaceWeatherBlock.Temperature,
        -> SpaceWeatherRefreshSource.Plasma
        SpaceWeatherBlock.Goes -> SpaceWeatherRefreshSource.GoesMagnetometer
        SpaceWeatherBlock.HemisphericPower -> SpaceWeatherRefreshSource.HemisphericPower
        else -> null
    }

fun spaceWeatherBlocks(expanded: Boolean): List<List<SpaceWeatherBlock>> {
    val head =
        listOf(
            listOf(SpaceWeatherBlock.AuroraHero),
            listOf(SpaceWeatherBlock.SolarWindHeading),
            listOf(SpaceWeatherBlock.Metrics),
            listOf(SpaceWeatherBlock.Timeframe),
        )
    val solar =
        if (expanded) {
            listOf(
                listOf(SpaceWeatherBlock.BzBt, SpaceWeatherBlock.Density),
                listOf(SpaceWeatherBlock.Speed, SpaceWeatherBlock.Temperature),
                listOf(SpaceWeatherBlock.Goes),
            )
        } else {
            listOf(
                SpaceWeatherBlock.BzBt,
                SpaceWeatherBlock.Density,
                SpaceWeatherBlock.Speed,
                SpaceWeatherBlock.Temperature,
                SpaceWeatherBlock.Goes,
            ).map { listOf(it) }
        }
    val geo =
        listOf(listOf(SpaceWeatherBlock.GeomagneticHeading)) +
            if (expanded) {
                listOf(listOf(SpaceWeatherBlock.Kp, SpaceWeatherBlock.HemisphericPower))
            } else {
                listOf(listOf(SpaceWeatherBlock.Kp), listOf(SpaceWeatherBlock.HemisphericPower))
            }
    return head + solar + geo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceWeatherScreen(
    state: SpaceWeatherUiState,
    expanded: Boolean,
    onTimeframe: (Timeframe) -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onRefreshSource: ((SpaceWeatherRefreshSource) -> Unit)? = null,
) {
    var globeTouchActive by remember { mutableStateOf(false) }
    val magnetic = state.magnetic.screenData().orEmpty()
    val plasma = state.plasma.screenData().orEmpty()
    val kp = state.kp.screenData().orEmpty()
    val goes = state.goesMagnetometer.screenData()
    val hp = state.hemisphericPower.screenData().orEmpty()
    val aurora = auroraGlobePresentation(state.aurora)
    val chartDomain = spaceWeatherChartDomain(state.timeframe, nowMillis)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().testTag("space-weather-pull-refresh"),
    ) {
        // SceneView's AndroidView is cancelled if this LazyColumn consumes MOVE events mid-gesture.
        // Disable list scrolling for the lifetime of a globe touch stream so orbit/pinch remains intact.
        LazyColumn(
            Modifier
                .fillMaxSize()
                .testTag("space-weather-screen")
                .padding(horizontal = 16.dp, vertical = 12.dp),
            userScrollEnabled = !globeTouchActive,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            spaceWeatherBlocks(expanded).forEach { row ->
                item {
                    if (row.size == 1) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .testTag(if (expanded) "space-weather-expanded" else "space-weather-compact")
                        ) {
                            SpaceWeatherBlockContent(
                                row.first(),
                                state,
                                magnetic,
                                plasma,
                                kp,
                                goes,
                                hp,
                                aurora,
                                onTimeframe,
                                nowMillis,
                                chartDomain,
                                onRefreshSource,
                            ) { globeTouchActive = it }
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().testTag("space-weather-expanded"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { block ->
                                Box(Modifier.weight(1f)) {
                                    SpaceWeatherBlockContent(
                                        block,
                                        state,
                                        magnetic,
                                        plasma,
                                        kp,
                                        goes,
                                        hp,
                                        aurora,
                                        onTimeframe,
                                        nowMillis,
                                        chartDomain,
                                        onRefreshSource,
                                    ) { globeTouchActive = it }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceWeatherBlockContent(
    block: SpaceWeatherBlock,
    state: SpaceWeatherUiState,
    magnetic: List<SolarWindMag>,
    plasma: List<SolarWindPlasma>,
    kp: List<KpSample>,
    goes: GoesMagnetometerSeries?,
    hp: List<HemisphericPowerSample>,
    aurora: AuroraGlobePresentation,
    onTimeframe: (Timeframe) -> Unit,
    nowMillis: Long,
    chartDomain: ChartDomain,
    onRefreshSource: ((SpaceWeatherRefreshSource) -> Unit)?,
    onGlobeTouchActiveChanged: (Boolean) -> Unit,
) {
    val refreshSource = refreshSourceForBlock(block)
    val onChartRefresh: (() -> Unit)? =
        if (refreshSource != null && onRefreshSource != null) {
            { onRefreshSource(refreshSource) }
        } else {
            null
        }
    val chartRefreshing = refreshSource != null && refreshSource in state.refreshingSources

    when (block) {
        SpaceWeatherBlock.AuroraHero ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .testTag("aurora-globe-slot")
            ) {
                AuroraGlobe(
                    modifier = Modifier.fillMaxSize(),
                    points = aurora.points,
                    onTouchActiveChanged = onGlobeTouchActiveChanged,
                )
                aurora.freshness?.let {
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        FreshnessIndicator(it)
                    }
                }
            }
        SpaceWeatherBlock.SolarWindHeading -> SectionHeading("Solar Wind")
        SpaceWeatherBlock.Metrics -> SolarWindMetrics(latestSolarWindMetrics(magnetic, plasma))
        SpaceWeatherBlock.Timeframe -> TimeframeSelector(state.timeframe, onTimeframe)
        SpaceWeatherBlock.BzBt ->
            SpaceWeatherLineCard(
                bzBtChartMeta,
                bzBtSeries(magnetic, state.timeframe, nowMillis),
                chartDomain,
                referenceLines = listOf(ChartReferenceLine(0.0, style = ChartReferenceStyle.Neutral)),
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
                refreshContentDescription = "Refresh IMF Bz / Bt",
            )
        SpaceWeatherBlock.Density ->
            SpaceWeatherLineCard(
                densityChartMeta,
                plasmaSeries(
                    plasma,
                    state.timeframe,
                    nowMillis,
                    label = "Density",
                    style = ChartSeriesStyle.Warning,
                ) { it.density },
                chartDomain,
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
                refreshContentDescription = "Refresh Density",
            )
        SpaceWeatherBlock.Speed ->
            SpaceWeatherLineCard(
                speedChartMeta,
                plasmaSeries(
                    plasma,
                    state.timeframe,
                    nowMillis,
                    label = "Speed",
                    style = ChartSeriesStyle.Success,
                ) { it.speed },
                chartDomain,
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
                refreshContentDescription = "Refresh Speed",
            )
        SpaceWeatherBlock.Temperature ->
            SpaceWeatherLineCard(
                temperatureChartMeta,
                plasmaSeries(
                    plasma,
                    state.timeframe,
                    nowMillis,
                    label = "Temperature",
                    style = ChartSeriesStyle.Primary,
                ) { it.temperature },
                chartDomain,
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
                refreshContentDescription = "Refresh Temperature",
            )
        SpaceWeatherBlock.Goes ->
            SpaceWeatherLineCard(
                goesChartMeta(goes?.primaryLabel, goes?.secondaryLabel),
                goesSeriesOrEmpty(goes, state.timeframe, nowMillis),
                chartDomain,
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
                refreshContentDescription = "Refresh GOES magnetometer",
            )
        SpaceWeatherBlock.GeomagneticHeading -> SectionHeading("Geomagnetic Activity")
        SpaceWeatherBlock.Kp -> KpChart(kpPresentation(kp, nowMillis), kpChartDomain(nowMillis))
        SpaceWeatherBlock.HemisphericPower ->
            HemisphericPowerChart(
                hemisphericPowerSeries(hp),
                onRefresh = onChartRefresh,
                refreshing = chartRefreshing,
            )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
