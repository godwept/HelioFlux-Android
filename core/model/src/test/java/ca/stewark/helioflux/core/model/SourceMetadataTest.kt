package ca.stewark.helioflux.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceMetadataTest {
    @Test
    fun `data freshness supports planned states`() {
        assertEquals(
            listOf(
                DataFreshness.Fresh,
                DataFreshness.Delayed,
                DataFreshness.Cached,
            ),
            DataFreshness.entries,
        )
    }

    @Test
    fun `source metadata keeps observation and fetched timestamps separate`() {
        val metadata = SourceMetadata(
            source = DataSourceKey("noaa-solar-wind"),
            observationTimestampMillis = 1_725_000_000_000L,
            fetchedTimestampMillis = 1_725_000_120_000L,
            freshness = DataFreshness.Fresh,
        )

        assertEquals(1_725_000_000_000L, metadata.observationTimestampMillis)
        assertEquals(1_725_000_120_000L, metadata.fetchedTimestampMillis)
        assertNotEquals(metadata.observationTimestampMillis, metadata.fetchedTimestampMillis)
    }
}
