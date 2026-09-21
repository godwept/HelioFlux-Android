package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.ChartYDomain
import org.junit.Assert.*
import org.junit.Test

class KpChartTest {
    @Test
    fun thresholdBoundariesUseSharedMapper() {
        assertEquals(KpStatus.Unsettled, kpStatus(3.0))
        assertEquals(KpStatus.Minor, kpStatus(5.0))
        assertEquals(KpStatus.Moderate, kpStatus(6.0))
        assertEquals(KpStatus.Strong, kpStatus(7.0))
        assertEquals(KpStatus.Severe, kpStatus(9.0))
    }

    @Test
    fun currentStatusUsesLatestNonNullKp() {
        val now = 10_000L
        val presentation = kpPresentation(
            listOf(KpSample(now - 2, 4.0), KpSample(now - 1, 6.0)),
            now,
        )

        assertEquals(KpStatus.Moderate, presentation.currentStatus)
    }

    @Test
    fun kpAlwaysUsesRollingTwoDayWindow() {
        val now = 300_000_000L
        val inside = KpSample(now - 36 * 60 * 60 * 1000L, 4.0)
        val outside = KpSample(now - 60 * 60 * 60 * 1000L, 7.0)
        val presentation = kpPresentation(listOf(outside, inside), now)
        assertEquals(listOf(4.0), presentation.values.map { it.second })
        assertEquals(
            ca.stewark.helioflux.ui.components.ChartDomain(
                (now - Timeframe.TwoDays.durationMillis).toDouble(),
                now.toDouble(),
            ),
            kpChartDomain(now),
        )
    }

    @Test
    fun kpUsesFixedNaturalScale() {
        assertEquals(ChartYDomain(0.0, 9.0), KpYDomain)
    }

    @Test
    fun presentationCopyIsStable() {
        assertEquals("Geomagnetic Activity", kpChartMeta.context)
        assertEquals("Planetary Kp Index (3-hour)", kpChartMeta.title)
        assertEquals("No current Kp", kpStatusLabel(null))
    }
}
