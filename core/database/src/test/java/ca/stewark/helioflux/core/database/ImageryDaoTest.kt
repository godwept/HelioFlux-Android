package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.ActiveRegionEntity
import ca.stewark.helioflux.core.database.entity.EnlilFrameEntity
import ca.stewark.helioflux.core.database.entity.SolarImageEntity
import ca.stewark.helioflux.core.model.SolarImageType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageryDaoTest {
    @Test fun storesLatestImageMetadataOnly() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val type = SolarImageType.entries.first()
            db.solarImageDao().upsert(SolarImageEntity(type, 100, "https://example.invalid/old.jpg"))
            db.solarImageDao().upsert(SolarImageEntity(type, 200, "https://example.invalid/new.jpg"))
            val image = db.solarImageDao().getLatest(type)!!
            assertEquals(200L, image.sourceTimestampMillis)
            assertEquals("https://example.invalid/new.jpg", image.url)
        } finally { db.close() }
    }

    @Test fun replacesCurrentActiveRegions() = runBlocking {
        val db = inMemoryDatabase()
        try {
            db.activeRegionDao().replaceAll(listOf(ActiveRegionEntity("a", "123", 1.0, 2.0), ActiveRegionEntity("b", null, 3.0, 4.0)))
            db.activeRegionDao().replaceAll(listOf(ActiveRegionEntity("c", "456", 5.0, 6.0)))
            assertEquals(listOf("c"), db.activeRegionDao().getAll().map { it.id })
        } finally { db.close() }
    }

    @Test fun returnsEnlilFramesInRunAndFrameOrder() = runBlocking {
        val db = inMemoryDatabase()
        try {
            db.enlilFrameDao().upsertAll(listOf(
                EnlilFrameEntity(100, 300, "c"),
                EnlilFrameEntity(200, 250, "b"),
                EnlilFrameEntity(200, 150, "a"),
            ))
            val frames = db.enlilFrameDao().getOrdered()
            assertEquals(listOf(150L, 250L, 300L), frames.map { it.frameTimestampMillis })
            db.enlilFrameDao().deleteOtherRuns(200)
            assertEquals(listOf(150L, 250L), db.enlilFrameDao().getOrdered().map { it.frameTimestampMillis })
        } finally { db.close() }
    }
}
