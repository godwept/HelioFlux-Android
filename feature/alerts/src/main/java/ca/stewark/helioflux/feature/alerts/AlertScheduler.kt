package ca.stewark.helioflux.feature.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun interface AlertScheduling {
    fun schedule()
}

internal val alertWorkConstraints: Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

internal fun buildAlertWorkRequest(): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<SpaceWeatherAlertWorker>(1, TimeUnit.HOURS)
        .setConstraints(alertWorkConstraints)
        .build()

class AlertScheduler(private val context: Context) : AlertScheduling {
    override fun schedule() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildAlertWorkRequest(),
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "space-weather-alerts"
    }
}
