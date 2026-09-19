package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpResult
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.database.dao.AuroraSnapshotDao
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.entity.AuroraPointEntity
import ca.stewark.helioflux.core.database.entity.AuroraSnapshotEntity
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuroraRepositoryTest {
    @Test fun cachedSnapshotEmitsThenValidRefreshStoresCompleteSnapshot() = runTest {
        val dao = FakeAuroraDao().apply {
            replaceSnapshot(
                AuroraSnapshotEntity(1_000, 2_000),
                listOf(AuroraPointEntity(1_000, 60.0, 10.0, 20.0)),
            )
        }
        val status = FakeStatusDao().apply {
            upsert(DataSourceStatusEntity(AuroraRepository.SOURCE, 1_000, 1_100, 1_100, 1_100, null, null))
        }
        val transport = FakeTransport().apply {
            body = """{"Observation Time":"1970-01-01T00:00:03Z","Forecast Time":"1970-01-01T00:00:04Z","coordinates":[[2,62,10],[4,64,12]]}"""
        }
        val repository = AuroraRepository(dao, status, transport) { 3_100 }

        val cached = repository.snapshot().first() as RepositoryState.Available
        assertEquals(1_000, cached.data.observationTimestampMillis)

        repository.refresh()

        val refreshed = repository.snapshot().first() as RepositoryState.Available
        assertEquals(3_000, refreshed.data.observationTimestampMillis)
        assertEquals(2, refreshed.data.points.size)
        assertEquals(DataFreshness.Fresh, refreshed.freshness)
    }

    @Test fun failedRefreshPreservesSnapshotAndChangesFreshnessOnly() = runTest {
        val dao = FakeAuroraDao().apply {
            replaceSnapshot(
                AuroraSnapshotEntity(1_000, 2_000),
                listOf(AuroraPointEntity(1_000, 60.0, 10.0, 20.0)),
            )
        }
        val status = FakeStatusDao().apply {
            upsert(DataSourceStatusEntity(AuroraRepository.SOURCE, 1_000, 1_100, 1_100, 1_100, null, null))
        }
        val repository = AuroraRepository(dao, status, FakeTransport().apply { body = "{}" }) { 3_100 }

        repository.refresh()

        val failed = repository.snapshot().first() as RepositoryState.Failure
        assertEquals(1_000, failed.retainedData?.observationTimestampMillis)
        assertEquals(DataFreshness.Cached, failed.freshness)
        assertNotNull(status.get(AuroraRepository.SOURCE)?.lastErrorTimestampMillis)
    }

    private class FakeAuroraDao : AuroraSnapshotDao() {
        private val snapshots = linkedMapOf<Long, AuroraSnapshotEntity>()
        private val points = linkedMapOf<Long, MutableList<AuroraPointEntity>>()

        override suspend fun upsertSnapshot(snapshot: AuroraSnapshotEntity) { snapshots[snapshot.observationTimestampMillis] = snapshot }
        override suspend fun upsertPoints(points: List<AuroraPointEntity>) {
            points.forEach { point -> this.points.getOrPut(point.snapshotTimestampMillis) { mutableListOf() }.add(point) }
        }
        override suspend fun deletePointsForSnapshot(snapshotTimestampMillis: Long) { points.remove(snapshotTimestampMillis) }
        override suspend fun latestSnapshot(): AuroraSnapshotEntity? = snapshots.values.maxByOrNull { it.observationTimestampMillis }
        override suspend fun pointsForSnapshot(snapshotTimestampMillis: Long): List<AuroraPointEntity> =
            points[snapshotTimestampMillis].orEmpty().sortedWith(compareBy({ it.latitude }, { it.longitude }))
        override suspend fun deleteOldSnapshotsExcept(cutoffMillis: Long, keepTimestampMillis: Long) {
            snapshots.keys.filter { it < cutoffMillis && it != keepTimestampMillis }.forEach { snapshots.remove(it); points.remove(it) }
        }
    }

    private class FakeTransport : HttpTransport {
        var body = ""
        override suspend fun get(url: String) = HttpResult(200, body, emptyMap())
        override suspend fun head(url: String) = HttpResult(200, "", emptyMap())
    }

    private class FakeStatusDao : DataSourceStatusDao {
        private val states = mutableMapOf<DataSourceKey, MutableStateFlow<DataSourceStatusEntity?>>()
        override suspend fun upsert(status: DataSourceStatusEntity) { states.getOrPut(status.source) { MutableStateFlow(null) }.value = status }
        override suspend fun get(source: DataSourceKey): DataSourceStatusEntity? = states[source]?.value
        override fun observe(source: DataSourceKey): Flow<DataSourceStatusEntity?> = states.getOrPut(source) { MutableStateFlow(null) }
    }
}
