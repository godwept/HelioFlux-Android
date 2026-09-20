package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.SolarHeroFrameEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SolarHeroFrameDaoTest {
    @Test fun replaceAllNeverPublishesAnEmptyIntermediateFrameSet() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val dao = db.solarHeroFrameDao()
            dao.replaceAll(listOf(SolarHeroFrameEntity("old", 1L, "old")))

            val nextEmission = async {
                withTimeout(1_000) { dao.observeAll().drop(1).first() }
            }
            dao.replaceAll(
                listOf(
                    SolarHeroFrameEntity("new-1", 2L, "new-1"),
                    SolarHeroFrameEntity("new-2", 3L, "new-2"),
                )
            )

            assertEquals(listOf("new-1", "new-2"), nextEmission.await().map { it.imageId })
        } finally {
            db.close()
        }
    }
}
