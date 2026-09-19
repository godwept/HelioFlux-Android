package ca.stewark.helioflux.core.database

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HelioFluxDatabaseTest {
    @Test
    fun opensAndClosesInMemoryDatabase() {
        val database = inMemoryDatabase()
        assertTrue(database.isOpen)
        database.close()
        assertTrue(!database.isOpen)
    }
}
