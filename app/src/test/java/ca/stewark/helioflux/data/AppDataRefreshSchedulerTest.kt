package ca.stewark.helioflux.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDataRefreshSchedulerTest {
    @Test
    fun schedulerUsesDistinctUniqueWorkNames() {
        assertNotEquals(
            AppDataRefreshScheduler.FAST_UNIQUE_WORK_NAME,
            AppDataRefreshScheduler.SLOW_UNIQUE_WORK_NAME,
        )
    }

    @Test
    fun schedulerKeepsOneFastAndOneSlowPeriodicWork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val scheduler = AppDataRefreshScheduler(context)

        scheduler.schedule()
        scheduler.schedule()

        val workManager = WorkManager.getInstance(context)
        val fast = workManager
            .getWorkInfosForUniqueWork(AppDataRefreshScheduler.FAST_UNIQUE_WORK_NAME)
            .get()
        val slow = workManager
            .getWorkInfosForUniqueWork(AppDataRefreshScheduler.SLOW_UNIQUE_WORK_NAME)
            .get()

        assertEquals(1, fast.size)
        assertEquals(WorkInfo.State.ENQUEUED, fast.single().state)
        assertEquals(1, slow.size)
        assertEquals(WorkInfo.State.ENQUEUED, slow.single().state)
    }
}
