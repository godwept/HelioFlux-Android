package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataSourceStatusDaoTest {
    @Test
    fun upsertsAndReadsStatusBySourceKey() = runBlocking {
        val database = inMemoryDatabase()
        try {
            val key = DataSourceKey("noaa-solar-wind")
            val first = DataSourceStatusEntity(key, 100, 110, 110, 110, null, null)
            val updated = first.copy(fetchedTimestampMillis = 120, lastAttemptTimestampMillis = 120)
            database.dataSourceStatusDao().upsert(first)
            database.dataSourceStatusDao().upsert(updated)

            assertEquals(updated, database.dataSourceStatusDao().get(key))
        } finally {
            database.close()
        }
    }
}
