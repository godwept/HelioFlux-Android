package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryStateTest {
    private val source = DataSourceKey("test")

    @Test
    fun contractRepresentsCachedLoadingFailureAndEmptyValidStates() {
        assertTrue(RepositoryState.Loading is RepositoryState.Loading)

        val cached = RepositoryState.Available(listOf(1), source, DataFreshness.Cached)
        assertEquals(DataFreshness.Cached, cached.freshness)

        val failed = RepositoryState.Failure(source, "offline", listOf(1), DataFreshness.Cached)
        assertEquals(source, failed.source)
        assertEquals(listOf(1), failed.retainedData)

        val empty = RepositoryState.Empty(source, DataFreshness.Fresh)
        assertEquals(source, empty.source)

        val noDataFailure = RepositoryState.Failure<List<Int>>(source, "bad response", null, null)
        assertNull(noDataFailure.retainedData)
    }
}
