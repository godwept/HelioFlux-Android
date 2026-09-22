package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.*
import org.junit.Test

class SolarActivityComponentsTest {
    @Test
    fun imageryPresentationKeepsMetadataAndFormatsUtcWhenAvailable() {
        val source = DataSourceKey("image")
        val loading =
            solarImageryPresentation(
                "LASCO C2",
                "SOHO / LASCO",
                RepositoryState.Loading,
            )
        assertEquals("LASCO C2", loading.title)
        assertEquals("SOHO / LASCO", loading.source)
        assertNull(loading.imageUrl)
        assertNull(loading.updatedTime)

        val available =
            solarImageryPresentation(
                "LASCO C2",
                "SOHO / LASCO",
                RepositoryState.Available(
                    SolarImage(
                        SolarImageType.LascoC2,
                        1789907640000L,
                        "https://example.test/c2.gif",
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
            )
        assertEquals("Sep 20, 12:34 UTC", available.updatedTime)
        assertEquals("https://example.test/c2.gif", available.imageUrl)
    }

    @Test
    fun magnetogramCoordinatesMatchPwaMapping() {
        assertEquals(NormalizedRegionPosition(0.5, 0.5), mapActiveRegion(0.0, 0.0))
        assertEquals(NormalizedRegionPosition(1.0, 0.0), mapActiveRegion(1000.0, 1000.0))
        assertEquals(NormalizedRegionPosition(0.0, 1.0), mapActiveRegion(-1000.0, -1000.0))
    }

    @Test
    fun activeRegionLabelSpacingUsesApprovedThresholds() {
        assertEquals(10.dp, ActiveRegionLabelMinGap)
        assertEquals(48.dp, ActiveRegionLabelMaxDisplacement)
        assertEquals(16.dp, ActiveRegionLeaderThreshold)
    }
}
