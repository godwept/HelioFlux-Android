package ca.stewark.helioflux.feature.widgets

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*

object WidgetModelMapper {
    fun spaceWeather(
        kp: RepositoryState<List<KpSample>>,
        magnetic: RepositoryState<List<SolarWindMag>>,
        plasma: RepositoryState<List<SolarWindPlasma>>,
        flare: RepositoryState<FlareProbabilities>,
    ) = SpaceWeatherWidgetModel(
        kp = kp.dataOrNull()?.lastOrNull()?.kp?.let { "Kp %.1f".format(it) } ?: "Kp --",
        bz = magnetic.dataOrNull()?.lastOrNull { it.bz != null }?.bz?.let { "Bz %.1f nT".format(it) } ?: "Bz --",
        speed = plasma.dataOrNull()?.lastOrNull { it.speed != null }?.speed?.let { "%.0f km/s".format(it) } ?: "-- km/s",
        flareStatus = flare.dataOrNull()?.let { "M "+it.m+"% · X "+it.x+"%" } ?: "M -- · X --",
        freshness = listOf(kp, magnetic, plasma, flare).combinedFreshness(),
    )

    fun aurora(
        kp: RepositoryState<List<KpSample>>,
        ovation: RepositoryState<AuroraSnapshot>,
        hemisphericPower: RepositoryState<List<HemisphericPowerSample>>,
    ) = AuroraConditionsWidgetModel(
        kp = kp.dataOrNull()?.lastOrNull()?.kp?.let { "Kp %.1f".format(it) } ?: "Kp --",
        ovationSummary = ovation.dataOrNull()?.points?.maxOfOrNull { it.probability }?.let { "OVATION max %.0f%%".format(it) } ?: "OVATION --",
        hemisphericPower = hemisphericPower.dataOrNull()?.lastOrNull()?.let { sample ->
            sample.north?.let { "North %.0f GW".format(it) }
        },
        freshness = listOf(kp, ovation, hemisphericPower).combinedFreshness(),
    )

    private fun RepositoryState<*>.freshnessOrNull(): DataFreshness? = when (this) {
        is RepositoryState.Available -> freshness
        is RepositoryState.Empty -> freshness
        is RepositoryState.Failure -> freshness
        RepositoryState.Loading -> null
    }

    private fun List<RepositoryState<*>>.combinedFreshness(): DataFreshness {
        val values = mapNotNull { it.freshnessOrNull() }
        return when {
            DataFreshness.Cached in values -> DataFreshness.Cached
            DataFreshness.Delayed in values -> DataFreshness.Delayed
            else -> DataFreshness.Fresh
        }
    }

    private fun <T> RepositoryState<T>.dataOrNull(): T? = when (this) {
        is RepositoryState.Available -> data
        is RepositoryState.Failure -> retainedData
        else -> null
    }
}
