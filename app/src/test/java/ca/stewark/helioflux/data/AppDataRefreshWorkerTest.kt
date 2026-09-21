package ca.stewark.helioflux.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDataRefreshWorkerTest {
    @Test
    fun fastRequestUsesFifteenMinutePeriodAndConnectedNetwork() {
        val request = buildFastDataRefreshRequest()

        assertEquals(TimeUnit.MINUTES.toMillis(15), request.workSpec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(AppDataRefreshWorker.TIER_FAST, request.workSpec.input.getString(AppDataRefreshWorker.KEY_TIER))
    }

    @Test
    fun slowRequestUsesOneHourPeriodAndConnectedNetwork() {
        val request = buildSlowDataRefreshRequest()

        assertEquals(TimeUnit.HOURS.toMillis(1), request.workSpec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(AppDataRefreshWorker.TIER_SLOW, request.workSpec.input.getString(AppDataRefreshWorker.KEY_TIER))
    }

    @Test
    fun fastWorkerCallsRefreshFast() = runTest {
        var fastCalls = 0
        var slowCalls = 0
        val coordinator = AppRefreshCoordinator(
            fastActions = listOf({ fastCalls++ }),
            slowActions = listOf({ slowCalls++ }),
            solarHeroAction = {},
        )

        val result = worker(AppDataRefreshWorker.TIER_FAST, coordinator).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, fastCalls)
        assertEquals(0, slowCalls)
    }

    @Test
    fun slowWorkerCallsRefreshSlow() = runTest {
        var fastCalls = 0
        var slowCalls = 0
        val coordinator = AppRefreshCoordinator(
            fastActions = listOf({ fastCalls++ }),
            slowActions = listOf({ slowCalls++ }),
            solarHeroAction = {},
        )

        val result = worker(AppDataRefreshWorker.TIER_SLOW, coordinator).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, fastCalls)
        assertEquals(1, slowCalls)
    }

    private fun worker(
        tier: String,
        coordinator: AppRefreshCoordinator,
    ): AppDataRefreshWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ) = AppDataRefreshWorker(appContext, workerParameters, coordinator)
        }
        return TestListenableWorkerBuilder<AppDataRefreshWorker>(context)
            .setInputData(workDataOf(AppDataRefreshWorker.KEY_TIER to tier))
            .setWorkerFactory(factory)
            .build()
    }
}
