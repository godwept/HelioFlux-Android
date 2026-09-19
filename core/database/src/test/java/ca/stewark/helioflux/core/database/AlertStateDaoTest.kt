package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.AlertStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlertStateDaoTest {
    @Test fun savesReadsAndAtomicallyReplacesAlertState() = runBlocking {
        val db = inMemoryDatabase()
        try {
            val dao = db.alertStateDao()
            assertNull(dao.get())
            dao.update(AlertStateEntity(lastNotifiedKpBand = 5, notifiedFlareIds = "flare-1"))
            assertEquals(5, dao.get()?.lastNotifiedKpBand)
            dao.update(AlertStateEntity(lastNotifiedKpBand = 6, notifiedFlareIds = "flare-1\nflare-2"))
            val updated = dao.get()
            assertEquals(6, updated?.lastNotifiedKpBand)
            assertEquals("flare-1\nflare-2", updated?.notifiedFlareIds)
        } finally { db.close() }
    }
}
