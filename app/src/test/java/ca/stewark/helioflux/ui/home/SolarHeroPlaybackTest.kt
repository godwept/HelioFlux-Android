package ca.stewark.helioflux.ui.home

import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.core.model.SolarImageType
import org.junit.Assert.assertEquals
import org.junit.Test

class SolarHeroPlaybackTest {
    private val frames = listOf(
        SolarImage(SolarImageType.Aia304, 1L, "one"),
        SolarImage(SolarImageType.Aia304, 2L, "two"),
        SolarImage(SolarImageType.Aia304, 3L, "three"),
    )

    @Test fun heroStaysLoadingUntilPreloadFinishes() {
        assertEquals(true, solarHeroLoading(frames, false))
        assertEquals(false, solarHeroLoading(frames, true))
    }

    @Test fun playbackUsesOnlySuccessfullyPreloadedFrames() {
        assertEquals(emptyList<SolarImage>(), selectPlayableSolarFrames(frames, emptySet()))
        assertEquals(listOf(frames[0], frames[2]), selectPlayableSolarFrames(frames, setOf("one", "three")))
    }

    @Test fun blendProgressMatchesPwaFrameInterval() {
        assertEquals(0f, solarBlendProgress(0L, 200L), 0f)
        assertEquals(0.5f, solarBlendProgress(100L, 200L), 0f)
        assertEquals(1f, solarBlendProgress(200L, 200L), 0f)
    }
}
