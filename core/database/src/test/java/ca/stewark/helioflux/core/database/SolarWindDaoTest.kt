package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.SolarWindMagEntity
import ca.stewark.helioflux.core.database.entity.SolarWindPlasmaEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SolarWindDaoTest {
    @Test
    fun magneticSamplesQueryAscendingAndDeleteBeforeCutoff() = runBlocking {
        val database = inMemoryDatabase()
        try {
            val dao = database.solarWindMagDao()
            dao.upsertAll(
                listOf(
                    SolarWindMagEntity(300, null, null, -2.0, 5.0),
                    SolarWindMagEntity(100, 1.0, 2.0, 3.0, 4.0),
                    SolarWindMagEntity(200, null, null, null, null),
                ),
            )

            assertEquals(listOf(100L, 200L, 300L), dao.range(100, 300).map { it.timestampMillis })
            assertEquals(1, dao.deleteBefore(200))
            assertEquals(listOf(200L, 300L), dao.range(0, 1_000).map { it.timestampMillis })
        } finally {
            database.close()
        }
    }

    @Test
    fun plasmaSamplesQueryAscendingAndDeleteBeforeCutoff() = runBlocking {
        val database = inMemoryDatabase()
        try {
            val dao = database.solarWindPlasmaDao()
            dao.upsertAll(
                listOf(
                    SolarWindPlasmaEntity(300, 4.0, 500.0, 100_000.0),
                    SolarWindPlasmaEntity(100, null, 450.0, null),
                    SolarWindPlasmaEntity(200, 3.0, null, 90_000.0),
                ),
            )

            assertEquals(listOf(100L, 200L, 300L), dao.range(100, 300).map { it.timestampMillis })
            assertEquals(1, dao.deleteBefore(200))
            assertEquals(listOf(200L, 300L), dao.range(0, 1_000).map { it.timestampMillis })
        } finally {
            database.close()
        }
    }
}
