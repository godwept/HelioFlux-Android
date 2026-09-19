package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.network.HttpResult
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.ForecastSectionDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.ForecastSectionEntity
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ForecastRepositoryTest {
    @Test fun cachedFourSectionsEmitThenValidRefreshReplacesThem() = runTest {
        val dao = FakeForecastDao().apply { upsertAll(cachedSections()) }
        val status = FakeStatusDao().apply {
            upsert(DataSourceStatusEntity(ForecastRepository.SOURCE, null, 1_000, 1_000, 1_000, null, null))
        }
        val transport = FakeTransport(validDiscussion())
        val repository = ForecastRepository(dao, status, transport) { 2_000 }

        val cached = repository.sections().first() as RepositoryState.Available
        assertEquals("old solar", cached.data.first().summary)

        repository.refresh()

        val refreshed = repository.sections().first() as RepositoryState.Available
        assertEquals(4, refreshed.data.size)
        assertEquals("new solar", refreshed.data.first().summary)
    }

    @Test fun malformedDiscussionDoesNotWipeCache() = runTest {
        val dao = FakeForecastDao().apply { upsertAll(cachedSections()) }
        val status = FakeStatusDao().apply {
            upsert(DataSourceStatusEntity(ForecastRepository.SOURCE, null, 1_000, 1_000, 1_000, null, null))
        }
        val repository = ForecastRepository(dao, status, FakeTransport("Solar Activity\n.24 hr Summary... partial")) { 2_000 }

        repository.refresh()

        val failed = repository.sections().first() as RepositoryState.Failure
        assertEquals(4, failed.retainedData?.size)
        assertEquals("old solar", failed.retainedData?.first()?.summary)
        assertEquals(DataFreshness.Cached, failed.freshness)
        assertNotNull(status.get(ForecastRepository.SOURCE)?.lastErrorTimestampMillis)
    }

    private fun cachedSections() = listOf(
        ForecastSectionEntity("solar", "Solar Activity", "old solar", "old", "old issue"),
        ForecastSectionEntity("particle", "Energetic Particle", "old particle", "old", "old issue"),
        ForecastSectionEntity("wind", "Solar Wind", "old wind", "old", "old issue"),
        ForecastSectionEntity("geospace", "Geospace", "old geospace", "old", "old issue"),
    )

    private fun validDiscussion() = """:Issued: 2026 Sep 19 1200 UTC
Solar Activity
.24 hr Summary... new solar
.Forecast... solar forecast
Energetic Particle
.24 hr Summary... new particle
.Forecast... particle forecast
Solar Wind
.24 hr Summary... new wind
.Forecast... wind forecast
Geospace
.24 hr Summary... new geospace
.Forecast... geospace forecast"""

    private class FakeForecastDao : ForecastSectionDao {
        private val rows = linkedMapOf<String, ForecastSectionEntity>()
        override suspend fun upsertAll(sections: List<ForecastSectionEntity>) { sections.forEach { rows[it.key] = it } }
        override suspend fun getAll(): List<ForecastSectionEntity> {
            val order = listOf("solar", "particle", "wind", "geospace")
            return rows.values.sortedBy { order.indexOf(it.key).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        }
    }

    private class FakeTransport(private val body: String) : HttpTransport {
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
