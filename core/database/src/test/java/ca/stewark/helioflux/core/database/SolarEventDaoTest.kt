package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.CmeEventEntity
import ca.stewark.helioflux.core.database.entity.FlareEventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SolarEventDaoTest {
    @Test fun flareEventsUpsertOrderAndPurge() = runBlocking {
        val db = inMemoryDatabase()
        try {
            db.flareEventDao().upsertAll(listOf(
                FlareEventEntity("f1", "M1.0", 100, "GOES", null, null),
                FlareEventEntity("f2", "X2.0", 300, "GOES", "AR123", "N10W20"),
            ))
            db.flareEventDao().upsertAll(listOf(FlareEventEntity("f1", "M1.5", 200, "GOES", null, null)))
            assertEquals(listOf("f2", "f1"), db.flareEventDao().getNewestFirst().map { it.id })
            assertEquals("M1.5", db.flareEventDao().getNewestFirst().last().flareClass)
            db.flareEventDao().deleteBefore(250)
            assertEquals(listOf("f2"), db.flareEventDao().getNewestFirst().map { it.id })
        } finally { db.close() }
    }

    @Test fun cmeEventsPreserveOptionalMetadataAndPurge() = runBlocking {
        val db = inMemoryDatabase()
        try {
            db.cmeEventDao().upsertAll(listOf(
                CmeEventEntity("c1", 100, null, null, null, null, null),
                CmeEventEntity("c2", 300, 900.0, 45.0, "NW", "C", "https://example.invalid/c2"),
            ))
            val events = db.cmeEventDao().getNewestFirst()
            assertEquals(listOf("c2", "c1"), events.map { it.id })
            assertNull(events.last().speed)
            db.cmeEventDao().deleteBefore(200)
            assertEquals(listOf("c2"), db.cmeEventDao().getNewestFirst().map { it.id })
        } finally { db.close() }
    }
}
