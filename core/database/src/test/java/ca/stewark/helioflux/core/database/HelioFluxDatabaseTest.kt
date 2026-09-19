package ca.stewark.helioflux.core.database

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HelioFluxDatabaseTest {
    @Test
    fun opensAndClosesInMemoryDatabase() {
        val database = inMemoryDatabase()
        assertTrue(database.isOpen)
        database.close()
        assertTrue(!database.isOpen)
    }
}
