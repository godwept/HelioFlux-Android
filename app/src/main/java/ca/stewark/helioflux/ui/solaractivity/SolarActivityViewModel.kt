package ca.stewark.helioflux.ui.solaractivity

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.data.repository.SolarActivityRepository
import ca.stewark.helioflux.core.data.repository.SolarImageryRepository
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.chartDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal const val SolarActivityWindowMillis = 72L * 60L * 60L * 1_000L

fun solarActivityChartDomain(now: Long): ChartDomain =
    chartDomain(now, SolarActivityWindowMillis)

data class SolarActivityUiState(
    val probabilities: RepositoryState<FlareProbabilities> = RepositoryState.Loading,
    val magnetogram: RepositoryState<SolarImage> = RepositoryState.Loading,
    val lascoC2: RepositoryState<SolarImage> = RepositoryState.Loading,
    val lascoC3: RepositoryState<SolarImage> = RepositoryState.Loading,
    val activeRegions: RepositoryState<List<ActiveRegion>> = RepositoryState.Loading,
    val enlil: RepositoryState<List<EnlilFrame>> = RepositoryState.Loading,
    val xray: RepositoryState<List<XrayFluxSample>> = RepositoryState.Loading,
    val flares: RepositoryState<List<FlareEvent>> = RepositoryState.Loading,
    val cmes: RepositoryState<List<CmeEvent>> = RepositoryState.Loading,
    val epam: RepositoryState<List<AceEpamSample>> = RepositoryState.Loading,
)

class SolarActivityViewModel private constructor(
    probabilities: Flow<RepositoryState<FlareProbabilities>>,
    magnetogram: Flow<RepositoryState<SolarImage>>,
    lascoC2: Flow<RepositoryState<SolarImage>>,
    lascoC3: Flow<RepositoryState<SolarImage>>,
    activeRegions: Flow<RepositoryState<List<ActiveRegion>>>,
    enlil: Flow<RepositoryState<List<EnlilFrame>>>,
    xray: Flow<RepositoryState<List<XrayFluxSample>>>,
    flares: Flow<RepositoryState<List<FlareEvent>>>,
    cmes: Flow<RepositoryState<List<CmeEvent>>>,
    epam: Flow<RepositoryState<List<AceEpamSample>>>,
    scope: CoroutineScope,
) {
    val state: StateFlow<SolarActivityUiState> = combine(
        probabilities,
        magnetogram,
        lascoC2,
        lascoC3,
        activeRegions,
        enlil,
        xray,
        flares,
        cmes,
        epam,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SolarActivityUiState(
            probabilities = values[0] as RepositoryState<FlareProbabilities>,
            magnetogram = values[1] as RepositoryState<SolarImage>,
            lascoC2 = values[2] as RepositoryState<SolarImage>,
            lascoC3 = values[3] as RepositoryState<SolarImage>,
            activeRegions = values[4] as RepositoryState<List<ActiveRegion>>,
            enlil = values[5] as RepositoryState<List<EnlilFrame>>,
            xray = values[6] as RepositoryState<List<XrayFluxSample>>,
            flares = values[7] as RepositoryState<List<FlareEvent>>,
            cmes = values[8] as RepositoryState<List<CmeEvent>>,
            epam = values[9] as RepositoryState<List<AceEpamSample>>,
        )
    }.stateIn(scope, SharingStarted.Eagerly, SolarActivityUiState())

    constructor(
        activity: SolarActivityRepository,
        imagery: SolarImageryRepository,
        scope: CoroutineScope,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) : this(
        activity.flareProbabilities(),
        imagery.image(SolarImageType.Magnetogram),
        imagery.image(SolarImageType.LascoC2),
        imagery.image(SolarImageType.LascoC3),
        imagery.regions(),
        imagery.enlil(),
        activity.xray(nowMillis() - SolarActivityWindowMillis, nowMillis()),
        activity.flares(),
        activity.cmes(),
        activity.aceEpam(nowMillis() - SolarActivityWindowMillis, nowMillis()),
        scope,
    )

    companion object {
        internal fun forTest(
            probabilities: Flow<RepositoryState<FlareProbabilities>>,
            magnetogram: Flow<RepositoryState<SolarImage>>,
            lascoC2: Flow<RepositoryState<SolarImage>>,
            lascoC3: Flow<RepositoryState<SolarImage>>,
            activeRegions: Flow<RepositoryState<List<ActiveRegion>>>,
            enlil: Flow<RepositoryState<List<EnlilFrame>>>,
            xray: Flow<RepositoryState<List<XrayFluxSample>>>,
            flares: Flow<RepositoryState<List<FlareEvent>>>,
            cmes: Flow<RepositoryState<List<CmeEvent>>>,
            epam: Flow<RepositoryState<List<AceEpamSample>>>,
            scope: CoroutineScope,
        ) = SolarActivityViewModel(
            probabilities,
            magnetogram,
            lascoC2,
            lascoC3,
            activeRegions,
            enlil,
            xray,
            flares,
            cmes,
            epam,
            scope,
        )
    }
}
