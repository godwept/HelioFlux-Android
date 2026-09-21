package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

class SpaceWeatherViewModelTest {
    private fun loadingViewModel(
        scope: CoroutineScope,
        refreshActions: Map<SpaceWeatherRefreshSource, suspend () -> Unit> = emptyMap(),
    ) = SpaceWeatherViewModel.forTest(
        flowOf(RepositoryState.Loading),
        flowOf(RepositoryState.Loading),
        flowOf(RepositoryState.Loading),
        flowOf(RepositoryState.Loading),
        flowOf(RepositoryState.Loading),
        flowOf(RepositoryState.Loading),
        scope,
        refreshActions,
    )

    @Test
    fun uiStateDefaultsToTwelveHours() {
        assertEquals(Timeframe.TwelveHours, SpaceWeatherUiState().timeframe)
    }

    @Test
    fun viewModelEmitsTwelveHoursBeforeSelection() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = loadingViewModel(scope)
        yield()
        assertEquals(Timeframe.TwelveHours, vm.state.value.timeframe)
        scope.cancel()
    }

    @Test
    fun combinedCachedStateIncludesMetricsTimeframeSeriesAuroraAndFreshness() = runBlocking {
        val source = DataSourceKey("cached")
        val freshness = DataFreshness.Cached
        val magnetic = listOf(SolarWindMag(1, null, null, -4.5, 6.0))
        val plasma = listOf(SolarWindPlasma(1, 7.2, 455.0, 100000.0))
        val kp = listOf(KpSample(1, 5.0))
        val hp = listOf(HemisphericPowerSample(1, 42.0, 38.0))
        val goes = GoesMagnetometerSeries(listOf(GoesMagSample(1, 101.0, 99.0)), "GOES-19", "GOES-18")
        val aurora = AuroraSnapshot(1, 2, emptyList())
        fun <T> cached(data: T): RepositoryState<T> =
            RepositoryState.Failure(source, "offline", data, freshness)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = SpaceWeatherViewModel.forTest(
            flowOf(cached(magnetic)),
            flowOf(cached(plasma)),
            flowOf(cached(kp)),
            flowOf(cached(goes)),
            flowOf(cached(hp)),
            flowOf(cached(aurora)),
            scope,
        )
        yield()
        vm.selectTimeframe(Timeframe.TwelveHours)
        yield()
        val state = vm.state.value

        assertEquals(-4.5, state.currentBz!!, 0.0)
        assertEquals(455.0, state.currentSpeed!!, 0.0)
        assertEquals(7.2, state.currentDensity!!, 0.0)
        assertEquals(Timeframe.TwelveHours, state.timeframe)
        assertEquals(magnetic, (state.magnetic as RepositoryState.Failure).retainedData)
        assertEquals(kp, (state.kp as RepositoryState.Failure).retainedData)
        assertEquals(hp, (state.hemisphericPower as RepositoryState.Failure).retainedData)
        assertEquals(goes, (state.goesMagnetometer as RepositoryState.Failure).retainedData)
        assertEquals(aurora, (state.aurora as RepositoryState.Failure).retainedData)
        assertEquals(DataFreshness.Cached, state.freshness[source])
        scope.cancel()
    }

    @Test
    fun targetedRefreshRoutesToMatchingSources() = runBlocking {
        val calls = mutableListOf<SpaceWeatherRefreshSource>()
        val actions = SpaceWeatherRefreshSource.entries.associateWith { source ->
            suspend { calls += source }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = loadingViewModel(scope, actions)

        SpaceWeatherRefreshSource.entries.forEach(vm::refresh)
        yield()

        assertEquals(SpaceWeatherRefreshSource.entries.toSet(), calls.toSet())
        scope.cancel()
    }

    @Test
    fun duplicateTapForSameSourceIsIgnoredWhileRefreshing() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = loadingViewModel(
            scope,
            mapOf(
                SpaceWeatherRefreshSource.Magnetic to suspend {
                    calls++
                    gate.await()
                }
            ),
        )

        vm.refresh(SpaceWeatherRefreshSource.Magnetic)
        vm.refresh(SpaceWeatherRefreshSource.Magnetic)
        yield()

        assertEquals(1, calls)
        assertTrue(SpaceWeatherRefreshSource.Magnetic in vm.state.value.refreshingSources)

        gate.complete(Unit)
        yield()
        assertFalse(SpaceWeatherRefreshSource.Magnetic in vm.state.value.refreshingSources)
        scope.cancel()
    }

    @Test
    fun unrelatedSourcesCanRefreshAtTheSameTime() = runBlocking {
        val magneticGate = CompletableDeferred<Unit>()
        val plasmaGate = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = loadingViewModel(
            scope,
            mapOf(
                SpaceWeatherRefreshSource.Magnetic to suspend { magneticGate.await() },
                SpaceWeatherRefreshSource.Plasma to suspend { plasmaGate.await() },
            ),
        )

        vm.refresh(SpaceWeatherRefreshSource.Magnetic)
        vm.refresh(SpaceWeatherRefreshSource.Plasma)
        yield()

        assertEquals(
            setOf(SpaceWeatherRefreshSource.Magnetic, SpaceWeatherRefreshSource.Plasma),
            vm.state.value.refreshingSources,
        )

        magneticGate.complete(Unit)
        plasmaGate.complete(Unit)
        yield()
        assertTrue(vm.state.value.refreshingSources.isEmpty())
        scope.cancel()
    }
}
