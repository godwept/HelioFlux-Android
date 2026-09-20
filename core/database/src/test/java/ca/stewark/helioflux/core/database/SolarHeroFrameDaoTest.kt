package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.SolarHeroFrameEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SolarHeroFrameDaoTest {
    @Test fun replaceAllPublishesOnlyTheReplacementFrameSet() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val dao = db.solarHeroFrameDao()
            dao.replaceAll(listOf(SolarHeroFrameEntity("old", 1L, "old")))

            dao.replaceAll(
                listOf(
                    SolarHeroFrameEntity("new-1", 2L, "new-1"),
                    SolarHeroFrameEntity("new-2", 3L, "new-2"),
                )
            )

            assertEquals(
                listOf("new-1", "new-2"),
                dao.observeAll().first().map { it.imageId },
            )
        } finally {
            db.close()
        }
    }
}
