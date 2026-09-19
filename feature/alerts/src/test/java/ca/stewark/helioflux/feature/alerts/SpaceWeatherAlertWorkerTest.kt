package ca.stewark.helioflux.feature.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpaceWeatherAlertWorkerTest {
    @Test fun postsOnlyNewAlertsAndPersistsState() = runTest {
        val store = FakeStore(AlertState(lastKpBand = 5, notifiedFlareIds = setOf("old")))
        val poster = FakePoster()
        val worker = worker(
            FakeRepository(kp = 6.2, flares = listOf(AlertFlare("old", "M1.0"), AlertFlare("new", "X2.1"))),
            store,
            poster,
        )

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        assertEquals(listOf("kp:6:true", "flare:new:X2.1"), poster.posted)
        assertEquals(6, store.state.lastKpBand)
        assertEquals(setOf("old", "new"), store.state.notifiedFlareIds)
    }

    @Test fun transientFailureRetriesWithoutDuplicatingPreviouslyPostedAlerts() = runTest {
        val store = FakeStore(AlertState(lastKpBand = 6, notifiedFlareIds = setOf("seen")))
        val poster = FakePoster()
        val worker = worker(FakeRepository(error = TransientAlertDataException("offline")), store, poster)

        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
        assertEquals(emptyList<String>(), poster.posted)
        assertEquals(AlertState(6, setOf("seen")), store.state)
    }

    private fun worker(repository: AlertRepository, store: AlertStateStore, poster: AlertNotificationPoster): SpaceWeatherAlertWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dependencies = AlertWorkerDependencies(repository, store, poster)
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                SpaceWeatherAlertWorker(appContext, workerParameters, dependencies)
        }
        return TestListenableWorkerBuilder<SpaceWeatherAlertWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    private class FakeRepository(
        private val kp: Double? = null,
        private val flares: List<AlertFlare> = emptyList(),
        private val error: Exception? = null,
    ) : AlertRepository {
        override suspend fun currentConditions(): AlertConditions {
            error?.let { throw it }
            return AlertConditions(kp, flares)
        }
    }

    private class FakeStore(initial: AlertState) : AlertStateStore {
        var state = initial
        override suspend fun read() = state
        override suspend fun write(state: AlertState) { this.state = state }
    }

    private class FakePoster : AlertNotificationPoster {
        val posted = mutableListOf<String>()
        override fun postGeomagnetic(band: Int, escalation: Boolean) { posted += "kp:$band:$escalation" }
        override fun postFlare(id: String, flareClass: String) { posted += "flare:$id:$flareClass" }
    }
}
