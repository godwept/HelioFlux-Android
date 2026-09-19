package ca.stewark.helioflux.core.database

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HelioFluxDatabaseTest {
    @Test
    fun initializesAndClosesInMemoryDatabase() {
        val database = inMemoryDatabase()
        try {
            assertNotNull(database.openHelper.writableDatabase)
        } finally {
            database.close()
        }
    }
}
