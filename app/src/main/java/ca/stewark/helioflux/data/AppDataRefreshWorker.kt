package ca.stewark.helioflux.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

internal const val FastRefreshIntervalMinutes = 15L
internal const val SlowRefreshIntervalHours = 1L

internal val dataRefreshWorkConstraints: Constraints =
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal fun buildFastDataRefreshRequest(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<AppDataRefreshWorker>(
        FastRefreshIntervalMinutes,
        TimeUnit.MINUTES,
    )
        .setConstraints(dataRefreshWorkConstraints)
        .setInputData(workDataOf(AppDataRefreshWorker.KEY_TIER to AppDataRefreshWorker.TIER_FAST))
        .build()

internal fun buildSlowDataRefreshRequest(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<AppDataRefreshWorker>(
        SlowRefreshIntervalHours,
        TimeUnit.HOURS,
    )
        .setConstraints(dataRefreshWorkConstraints)
        .setInputData(workDataOf(AppDataRefreshWorker.KEY_TIER to AppDataRefreshWorker.TIER_SLOW))
        .build()

class AppDataRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val coordinatorOverride: AppRefreshCoordinator?,
) : CoroutineWorker(appContext, workerParameters) {
    constructor(
        appContext: Context,
        workerParameters: WorkerParameters,
    ) : this(appContext, workerParameters, null)

    override suspend fun doWork(): Result {
        val coordinator =
            coordinatorOverride
                ?: (applicationContext as? AppRefreshCoordinatorProvider)?.refreshCoordinator
                ?: return Result.failure()

        return when (inputData.getString(KEY_TIER)) {
            TIER_FAST -> {
                coordinator.refreshFast()
                Result.success()
            }
            TIER_SLOW -> {
                coordinator.refreshSlow()
                Result.success()
            }
            else -> Result.failure()
        }
    }

    companion object {
        internal const val KEY_TIER = "refresh-tier"
        internal const val TIER_FAST = "fast"
        internal const val TIER_SLOW = "slow"
    }
}
