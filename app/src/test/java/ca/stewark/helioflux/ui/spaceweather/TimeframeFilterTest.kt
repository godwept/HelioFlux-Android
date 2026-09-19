package ca.stewark.helioflux.ui.spaceweather

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeframeFilterTest {
    private val now = 10L * 24L * 60L * 60L * 1_000L

    @Test
    fun allTimeframes_includeBoundaryAndExcludeOlderSamples() {
        Timeframe.entries.forEach { timeframe ->
            val cutoff = now - timeframe.durationMillis
            val samples = listOf(cutoff - 1, cutoff, now, now + 1)

            val result = filterByTimeframe(samples, timeframe, now) { it }

            assertEquals(listOf(cutoff, now), result)
        }
    }
}
