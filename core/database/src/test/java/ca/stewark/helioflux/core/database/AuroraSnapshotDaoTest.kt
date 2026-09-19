package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.AuroraPointEntity
import ca.stewark.helioflux.core.database.entity.AuroraSnapshotEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AuroraSnapshotDaoTest {
    @Test fun storesSnapshotAndPointsAndReturnsLatestCompleteSnapshot() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val dao = db.auroraSnapshotDao()
            dao.replaceSnapshot(AuroraSnapshotEntity(100, 200), listOf(
                AuroraPointEntity(100, 50.0, -70.0, 20.0),
                AuroraPointEntity(100, 51.0, -69.0, 30.0),
            ))
            dao.replaceSnapshot(AuroraSnapshotEntity(300, 400), listOf(
                AuroraPointEntity(300, 52.0, -68.0, 40.0),
            ))
            val latest = dao.getLatestComplete()!!
            assertEquals(300L, latest.snapshot.observationTimestampMillis)
            assertEquals(listOf(40.0), latest.points.map { it.intensity })
        } finally { db.close() }
    }

    @Test fun replacingAndPurgingSnapshotsNeverDeletesLatestData() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val dao = db.auroraSnapshotDao()
            dao.replaceSnapshot(AuroraSnapshotEntity(100, 200), listOf(AuroraPointEntity(100, 50.0, -70.0, 10.0)))
            dao.replaceSnapshot(AuroraSnapshotEntity(300, 400), listOf(AuroraPointEntity(300, 51.0, -69.0, 20.0)))
            dao.replaceSnapshot(AuroraSnapshotEntity(300, 450), listOf(AuroraPointEntity(300, 52.0, -68.0, 30.0)))
            dao.deleteOld(500)
            val latest = dao.getLatestComplete()!!
            assertEquals(300L, latest.snapshot.observationTimestampMillis)
            assertEquals(450L, latest.snapshot.forecastTimestampMillis)
            assertEquals(listOf(30.0), latest.points.map { it.intensity })
        } finally { db.close() }
    }
}
