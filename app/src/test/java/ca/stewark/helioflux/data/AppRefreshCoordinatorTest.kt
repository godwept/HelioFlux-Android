package ca.stewark.helioflux.data

import ca.stewark.helioflux.RepositoryProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRefreshCoordinatorTest {
    @Test fun cachedStateIsNotBlockedAndRefreshStartsOnlyOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val coordinator = AppRefreshCoordinator(listOf({
            calls++
            gate.await()
        }))

        coordinator.refreshOnce(this)
        coordinator.refreshOnce(this)
        assertFalse(gate.isCompleted)
        advanceUntilIdle()

        assertEquals(1, calls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test fun oneSourceFailureDoesNotCancelOtherRefreshes() = runTest {
        val completed = mutableListOf<String>()
        val coordinator = AppRefreshCoordinator(
            listOf(
                { completed += "noaa" },
                { throw IllegalStateException("Helioviewer unavailable") },
                { completed += "charts" },
            )
        )

        coordinator.refreshOnce(this)
        advanceUntilIdle()

        assertTrue("NOAA refresh should survive an isolated failure", "noaa" in completed)
        assertTrue("Chart refresh should survive an isolated failure", "charts" in completed)
    }

    @Test fun startupRefreshLeavesSolarHeroToItsCacheAwareViewModelRefresh() {
        assertFalse(AppRefreshCoordinator.startupRefreshIncludesSolarHero)
    }
}
