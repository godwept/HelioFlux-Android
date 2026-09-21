package ca.stewark.helioflux.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager

fun interface AppDataRefreshScheduling {
    fun schedule()
}

class AppDataRefreshScheduler(private val context: Context) : AppDataRefreshScheduling {
    override fun schedule() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            FAST_UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildFastDataRefreshRequest(),
        )
        workManager.enqueueUniquePeriodicWork(
            SLOW_UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildSlowDataRefreshRequest(),
        )
    }

    companion object {
        const val FAST_UNIQUE_WORK_NAME = "helioflux-live-data-refresh"
        const val SLOW_UNIQUE_WORK_NAME = "helioflux-slow-data-refresh"
    }
}
