package ca.stewark.helioflux.ui.solaractivity

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarActivityViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun oneSectionFailureDoesNotMarkOtherSectionsFailed() = runTest {
        val loading = MutableStateFlow<RepositoryState<SolarImage>>(RepositoryState.Loading)
        val probability = MutableStateFlow<RepositoryState<FlareProbabilities>>(RepositoryState.Loading)
        val regions = MutableStateFlow<RepositoryState<List<ActiveRegion>>>(RepositoryState.Loading)
        val enlil = MutableStateFlow<RepositoryState<List<EnlilFrame>>>(RepositoryState.Loading)
        val xray = MutableStateFlow<RepositoryState<List<XrayFluxSample>>>(RepositoryState.Loading)
        val flares = MutableStateFlow<RepositoryState<List<FlareEvent>>>(RepositoryState.Loading)
        val cmes = MutableStateFlow<RepositoryState<List<CmeEvent>>>(RepositoryState.Loading)
        val epam = MutableStateFlow<RepositoryState<List<AceEpamSample>>>(RepositoryState.Loading)
        val vm = SolarActivityViewModel.forTest(
            probability, loading, loading, loading, regions, enlil, xray, flares, cmes, epam, this,
        )
        advanceUntilIdle()

        probability.value = RepositoryState.Failure(
            source = DataSourceKey("probability"),
            message = "failed",
            retainedData = null,
            freshness = null,
        )
        advanceUntilIdle()

        assertTrue(vm.state.value.probabilities is RepositoryState.Failure)
        assertTrue(vm.state.value.magnetogram is RepositoryState.Loading)
        assertTrue(vm.state.value.xray is RepositoryState.Loading)
        assertTrue(vm.state.value.flares is RepositoryState.Loading)
        assertTrue(vm.state.value.cmes is RepositoryState.Loading)
        assertTrue(vm.state.value.epam is RepositoryState.Loading)
    }
}
