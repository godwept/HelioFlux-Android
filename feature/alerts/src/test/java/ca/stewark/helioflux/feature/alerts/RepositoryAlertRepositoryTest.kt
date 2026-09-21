package ca.stewark.helioflux.feature.alerts

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.core.model.FlareEvent
import ca.stewark.helioflux.core.model.KpSample
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class RepositoryAlertRepositoryTest {
    private val source = DataSourceKey("cached")

    @Test
    fun currentConditionsReadsCachedKpAndFlaresOnly() = runTest {
        var kpReads = 0
        var flareReads = 0
        val repository = RepositoryAlertRepository.forTest(
            kpStates = { _, _ ->
                kpReads++
                flowOf(
                    RepositoryState.Available(
                        listOf(KpSample(1_000, 3.0), KpSample(2_000, 5.7)),
                        source,
                        DataFreshness.Fresh,
                    )
                )
            },
            flareStates = {
                flareReads++
                flowOf(
                    RepositoryState.Available(
                        listOf(FlareEvent("flare-1", "M2.4", 2_000, "GOES", null, null)),
                        source,
                        DataFreshness.Fresh,
                    )
                )
            },
            nowMillis = { 3_000 },
        )

        assertEquals(
            AlertConditions(5.7, listOf(AlertFlare("flare-1", "M2.4"))),
            repository.currentConditions(),
        )
        assertEquals(1, kpReads)
        assertEquals(1, flareReads)
    }

    @Test
    fun failedCachedSourceRemainsTransientFailure() = runTest {
        val repository = RepositoryAlertRepository.forTest(
            kpStates = { _, _ ->
                flowOf(
                    RepositoryState.Failure(
                        source,
                        "Kp unavailable",
                        listOf(KpSample(1_000, 4.0)),
                        DataFreshness.Cached,
                    )
                )
            },
            flareStates = {
                flowOf(
                    RepositoryState.Available(
                        emptyList(),
                        source,
                        DataFreshness.Fresh,
                    )
                )
            },
            nowMillis = { 3_000 },
        )

        try {
            repository.currentConditions()
            fail("Expected transient failure for failed cached Kp source")
        } catch (_: TransientAlertDataException) {
            // Expected.
        }
    }
}
