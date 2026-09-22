package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.imageloading.ImageDownloadProgress
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
                        "https://example.test/c2small.gif",
                    ),
                    source,
                    DataFreshness.Fresh,
                ),
            )
        assertEquals("Sep 20, 12:34 UTC", available.updatedTime)
        assertEquals("https://example.test/c2small.gif", available.imageUrl)
    }

    @Test
    fun availableImageStartsInMediaLoadingState() {
        assertEquals(
            SolarMediaLoadState.Loading,
            initialSolarMediaLoadState("https://example.test/c2small.gif"),
        )
    }

    @Test
    fun absentImageStartsInFailedMediaState() {
        assertEquals(SolarMediaLoadState.Failed, initialSolarMediaLoadState(null))
    }

    @Test
    fun mediaCallbacksTransitionToReadyOrFailed() {
        assertEquals(
            SolarMediaLoadState.Ready,
            reduceSolarMediaLoadState(SolarMediaLoadEvent.Success),
        )
        assertEquals(
            SolarMediaLoadState.Failed,
            reduceSolarMediaLoadState(SolarMediaLoadEvent.Error),
        )
    }

    @Test
    fun availableRepositoryImageStillShowsSpinnerUntilMediaReady() {
        assertTrue(
            shouldShowSolarMediaSpinner(
                repositoryLoading = false,
                imageUrl = "https://example.test/c2small.gif",
                mediaState = SolarMediaLoadState.Loading,
            ),
        )
        assertFalse(
            shouldShowSolarMediaSpinner(
                repositoryLoading = false,
                imageUrl = "https://example.test/c2small.gif",
                mediaState = SolarMediaLoadState.Ready,
            ),
        )
        assertFalse(
            shouldShowSolarMediaSpinner(
                repositoryLoading = false,
                imageUrl = "https://example.test/c2small.gif",
                mediaState = SolarMediaLoadState.Failed,
            ),
        )
    }

    @Test
    fun repositoryLoadingShowsSpinnerWithoutMediaUrl() {
        assertTrue(
            shouldShowSolarMediaSpinner(
                repositoryLoading = true,
                imageUrl = null,
                mediaState = SolarMediaLoadState.Failed,
            ),
        )
    }

    @Test
    fun solarDownloadProgressFormatsKnownTotalInKilobytes() {
        assertEquals(
            "Downloading… 8,420 KB / 17,860 KB",
            formatSolarDownloadProgress(
                ImageDownloadProgress(
                    bytesRead = 8_420L * 1024L,
                    totalBytes = 17_860L * 1024L,
                ),
            ),
        )
    }

    @Test
    fun solarDownloadProgressFormatsUnknownTotalWithDownloadedBytesOnly() {
        assertEquals(
            "Downloading… 8,420 KB",
            formatSolarDownloadProgress(
                ImageDownloadProgress(
                    bytesRead = 8_420L * 1024L,
                    totalBytes = null,
                ),
            ),
        )
    }

    @Test
    fun magnetogramCoordinatesMatchPwaMapping() {
        assertEquals(NormalizedRegionPosition(0.5, 0.5), mapActiveRegion(0.0, 0.0))
        assertEquals(NormalizedRegionPosition(1.0, 0.0), mapActiveRegion(1000.0, 1000.0))
        assertEquals(NormalizedRegionPosition(0.0, 1.0), mapActiveRegion(-1000.0, -1000.0))
    }

    @Test
    fun activeRegionLabelSpacingUsesSolarHamLikeThresholds() {
        assertEquals(10.dp, ActiveRegionLabelMinGap)
        assertEquals(6.dp, ActiveRegionAnchorGap)
        assertEquals(24.dp, ActiveRegionMaxHorizontalNudge)
        assertEquals(8.dp, ActiveRegionHorizontalNudgeStep)
    }
}
