package ca.stewark.helioflux.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRefreshCoordinatorTest {
    @Test
    fun fastRefreshRunsOnlyFastActions() = runTest {
        val calls = mutableListOf<String>()
        val coordinator = coordinator(
            fast = listOf({ calls += "magnetic" }, { calls += "plasma" }),
            slow = listOf({ calls += "forecast" }),
            solarHero = { calls += "hero" },
        )

        coordinator.refreshFast()

        assertEquals(listOf("magnetic", "plasma"), calls)
    }

    @Test
    fun slowRefreshRunsOnlySlowActions() = runTest {
        val calls = mutableListOf<String>()
        val coordinator = coordinator(
            fast = listOf({ calls += "magnetic" }),
            slow = listOf({ calls += "forecast" }, { calls += "flares" }),
            solarHero = { calls += "hero" },
        )

        coordinator.refreshSlow()

        assertEquals(listOf("forecast", "flares"), calls)
    }

    @Test
    fun allRefreshRunsFastSlowAndSolarHero() = runTest {
        val calls = mutableSetOf<String>()
        val coordinator = coordinator(
            fast = listOf({ calls += "fast" }),
            slow = listOf({ calls += "slow" }),
            solarHero = { calls += "hero" },
        )

        coordinator.refreshAll()

        assertEquals(setOf("fast", "slow", "hero"), calls)
    }

    @Test
    fun startupRefreshRunsFastAndSlowButNotSolarHeroAndStartsOnlyOnce() = runTest {
        val calls = mutableListOf<String>()
        val coordinator = coordinator(
            fast = listOf({ calls += "fast" }),
            slow = listOf({ calls += "slow" }),
            solarHero = { calls += "hero" },
        )

        coordinator.refreshStartupOnce(this)
        coordinator.refreshStartupOnce(this)
        advanceUntilIdle()

        assertEquals(listOf("fast", "slow"), calls)
        assertFalse(AppRefreshCoordinator.startupRefreshIncludesSolarHero)
    }

    @Test
    fun oneSourceFailureDoesNotCancelSiblingActions() = runTest {
        val calls = mutableSetOf<String>()
        val coordinator = coordinator(
            fast = listOf(
                { calls += "before" },
                { throw IllegalStateException("source unavailable") },
                { calls += "after" },
            ),
        )

        coordinator.refreshFast()

        assertTrue("first source should complete", "before" in calls)
        assertTrue("later source should survive sibling failure", "after" in calls)
    }

    @Test
    fun fastRefreshExecutionsAreSerialized() = runTest {
        val firstGate = CompletableDeferred<Unit>()
        var calls = 0
        val coordinator = coordinator(
            fast = listOf({
                calls++
                if (calls == 1) firstGate.await()
            }),
        )

        val first = launch { coordinator.refreshFast() }
        runCurrent()
        val second = launch { coordinator.refreshFast() }
        runCurrent()

        assertEquals(1, calls)

        firstGate.complete(Unit)
        first.join()
        second.join()

        assertEquals(2, calls)
    }

    @Test
    fun manualAllRefreshSetsBusyUntilEveryActionCompletes() = runTest {
        val gate = CompletableDeferred<Unit>()
        val coordinator = coordinator(fast = listOf({ gate.await() }))

        val refresh = launch { coordinator.refreshAll() }
        runCurrent()

        assertTrue(coordinator.isRefreshing.value)
        gate.complete(Unit)
        refresh.join()

        assertFalse(coordinator.isRefreshing.value)
    }

    @Test
    fun duplicateManualAllRefreshIsIgnoredWhileBusy() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val coordinator = coordinator(
            fast = listOf({
                calls++
                gate.await()
            }),
        )

        val first = launch { coordinator.refreshAll() }
        runCurrent()
        val duplicate = launch { coordinator.refreshAll() }
        runCurrent()

        assertEquals(1, calls)
        gate.complete(Unit)
        first.join()
        duplicate.join()
        assertFalse(coordinator.isRefreshing.value)
    }

    private fun coordinator(
        fast: List<suspend () -> Unit> = emptyList(),
        slow: List<suspend () -> Unit> = emptyList(),
        solarHero: suspend () -> Unit = {},
    ) = AppRefreshCoordinator(fast, slow, solarHero)
}
