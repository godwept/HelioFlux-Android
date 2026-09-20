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

    @Test fun playbackWaitsForPreloadedFramesAndKeepsPosterVisible() {
        assertEquals(listOf(frames.first()), selectPlayableSolarFrames(frames, emptySet()))
        assertEquals(listOf(frames[0], frames[2]), selectPlayableSolarFrames(frames, setOf("one", "three")))
    }
}
