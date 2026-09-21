package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SpaceWeatherRefreshSource {
    Magnetic,
    Plasma,
    GoesMagnetometer,
    HemisphericPower,
}

data class SpaceWeatherUiState(
    val magnetic: RepositoryState<List<SolarWindMag>> = RepositoryState.Loading,
    val plasma: RepositoryState<List<SolarWindPlasma>> = RepositoryState.Loading,
    val kp: RepositoryState<List<KpSample>> = RepositoryState.Loading,
    val goesMagnetometer: RepositoryState<GoesMagnetometerSeries> = RepositoryState.Loading,
    val hemisphericPower: RepositoryState<List<HemisphericPowerSample>> = RepositoryState.Loading,
    val aurora: RepositoryState<AuroraSnapshot> = RepositoryState.Loading,
    val timeframe: Timeframe = Timeframe.TwelveHours,
    val refreshingSources: Set<SpaceWeatherRefreshSource> = emptySet(),
) {
    val currentBz get() = magnetic.dataOrNull()?.lastOrNull { it.bz != null }?.bz
    val currentSpeed get() = plasma.dataOrNull()?.lastOrNull { it.speed != null }?.speed
    val currentDensity get() = plasma.dataOrNull()?.lastOrNull { it.density != null }?.density
    val freshness: Map<DataSourceKey, DataFreshness>
        get() =
            listOf(magnetic, plasma, kp, goesMagnetometer, hemisphericPower, aurora)
                .mapNotNull { it.sourceAndFreshness() }
                .toMap()
}

private fun <T> RepositoryState<T>.dataOrNull(): T? =
    when (this) {
        is RepositoryState.Available -> data
        is RepositoryState.Failure -> retainedData
        else -> null
    }

private fun RepositoryState<*>.sourceAndFreshness(): Pair<DataSourceKey, DataFreshness>? =
    when (this) {
        is RepositoryState.Available -> source to freshness
        is RepositoryState.Empty -> source to freshness
        is RepositoryState.Failure -> freshness?.let { source to it }
        RepositoryState.Loading -> null
    }

class SpaceWeatherViewModel private constructor(
    magnetic: Flow<RepositoryState<List<SolarWindMag>>>,
    plasma: Flow<RepositoryState<List<SolarWindPlasma>>>,
    kp: Flow<RepositoryState<List<KpSample>>>,
    goes: Flow<RepositoryState<GoesMagnetometerSeries>>,
    hemisphericPower: Flow<RepositoryState<List<HemisphericPowerSample>>>,
    aurora: Flow<RepositoryState<AuroraSnapshot>>,
    private val scope: CoroutineScope,
    private val refreshActions: Map<SpaceWeatherRefreshSource, suspend () -> Unit>,
) {
    private val selectedTimeframe = MutableStateFlow(Timeframe.TwelveHours)
    private val refreshingSources = MutableStateFlow<Set<SpaceWeatherRefreshSource>>(emptySet())

    val state: StateFlow<SpaceWeatherUiState> =
        combine(
            magnetic,
            plasma,
            kp,
            goes,
            hemisphericPower,
            aurora,
            selectedTimeframe,
            refreshingSources,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            SpaceWeatherUiState(
                magnetic = values[0] as RepositoryState<List<SolarWindMag>>,
                plasma = values[1] as RepositoryState<List<SolarWindPlasma>>,
                kp = values[2] as RepositoryState<List<KpSample>>,
                goesMagnetometer = values[3] as RepositoryState<GoesMagnetometerSeries>,
                hemisphericPower = values[4] as RepositoryState<List<HemisphericPowerSample>>,
                aurora = values[5] as RepositoryState<AuroraSnapshot>,
                timeframe = values[6] as Timeframe,
                refreshingSources = values[7] as Set<SpaceWeatherRefreshSource>,
            )
        }.stateIn(scope, SharingStarted.Eagerly, SpaceWeatherUiState())

    constructor(
        spaceWeather: SpaceWeatherRepository,
        auroraRepository: AuroraRepository,
        scope: CoroutineScope,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) : this(
        magnetic = spaceWeather.magnetic(nowMillis() - Timeframe.TwoDays.durationMillis, nowMillis()),
        plasma = spaceWeather.plasma(nowMillis() - Timeframe.TwoDays.durationMillis, nowMillis()),
        kp = spaceWeather.kp(nowMillis() - Timeframe.TwoDays.durationMillis, nowMillis()),
        goes = spaceWeather.goesMagnetometer(nowMillis() - Timeframe.TwoDays.durationMillis, nowMillis()),
        hemisphericPower = spaceWeather.hemisphericPower(nowMillis() - Timeframe.TwoDays.durationMillis, nowMillis()),
        aurora = auroraRepository.snapshot(),
        scope = scope,
        refreshActions = mapOf(
            SpaceWeatherRefreshSource.Magnetic to spaceWeather::refreshMagnetic,
            SpaceWeatherRefreshSource.Plasma to spaceWeather::refreshPlasma,
            SpaceWeatherRefreshSource.GoesMagnetometer to spaceWeather::refreshGoesMagnetometer,
            SpaceWeatherRefreshSource.HemisphericPower to spaceWeather::refreshHemisphericPower,
        ),
    )

    fun selectTimeframe(timeframe: Timeframe) {
        selectedTimeframe.value = timeframe
    }

    fun refresh(source: SpaceWeatherRefreshSource) {
        val action = refreshActions[source] ?: return
        if (source in refreshingSources.value) return
        refreshingSources.value = refreshingSources.value + source
        scope.launch {
            try {
                action()
            } finally {
                refreshingSources.value = refreshingSources.value - source
            }
        }
    }

    companion object {
        internal fun forTest(
            magnetic: Flow<RepositoryState<List<SolarWindMag>>>,
            plasma: Flow<RepositoryState<List<SolarWindPlasma>>>,
            kp: Flow<RepositoryState<List<KpSample>>>,
            goes: Flow<RepositoryState<GoesMagnetometerSeries>>,
            hemisphericPower: Flow<RepositoryState<List<HemisphericPowerSample>>>,
            aurora: Flow<RepositoryState<AuroraSnapshot>>,
            scope: CoroutineScope,
            refreshActions: Map<SpaceWeatherRefreshSource, suspend () -> Unit> = emptyMap(),
        ) = SpaceWeatherViewModel(
            magnetic,
            plasma,
            kp,
            goes,
            hemisphericPower,
            aurora,
            scope,
            refreshActions,
        )
    }
}
