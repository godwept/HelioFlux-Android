package ca.stewark.helioflux.feature.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlertSchedulerTest {
    @Test fun schedulesOneUniqueNetworkConnectedPeriodicWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val scheduler = AlertScheduler(context)

        scheduler.schedule()
        scheduler.schedule()

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(AlertScheduler.UNIQUE_WORK_NAME)
            .get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(NetworkType.CONNECTED, infos.single().constraints.requiredNetworkType)
    }
}
