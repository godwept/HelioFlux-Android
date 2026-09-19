package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.ForecastSectionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ForecastSectionDaoTest {
    @Test fun upsertsFourSectionsAndReplacesEachSectionByKey() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val initial = listOf(
                ForecastSectionEntity("solar", "Solar Activity", "s1", "f1", "issued-1"),
                ForecastSectionEntity("particle", "Energetic Particle", "s2", "f2", "issued-1"),
                ForecastSectionEntity("wind", "Solar Wind", "s3", "f3", "issued-1"),
                ForecastSectionEntity("geospace", "Geospace", "s4", "f4", "issued-1"),
            )
            db.forecastSectionDao().upsertAll(initial)
            db.forecastSectionDao().upsertAll(initial.map { it.copy(summary = it.summary + "-new", issueTime = "issued-2") })

            val stored = db.forecastSectionDao().getAll()
            assertEquals(listOf("solar", "particle", "wind", "geospace"), stored.map { it.key })
            assertEquals(4, stored.size)
            assertEquals(listOf("s1-new", "s2-new", "s3-new", "s4-new"), stored.map { it.summary })
            assertEquals(List(4) { "issued-2" }, stored.map { it.issueTime })
        } finally { db.close() }
    }
}
