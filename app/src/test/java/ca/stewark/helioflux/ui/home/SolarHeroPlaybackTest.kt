package ca.stewark.helioflux.ui.home

import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.core.model.SolarImageType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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


    @Test fun heroDoesNotLeaveLoadingWithOnlyOnePlayableFrame() {
        assertEquals(true, solarHeroLoading(listOf(frames[0]), true))
    }

    @Test fun loadingProgressUsesEnlilWordingAndOmitsEmptyTotals() {
        assertEquals("Loading frame 18 of 48", solarHeroLoadingText(18, 48))
        assertNull(solarHeroLoadingText(0, 0))
    }

    @Test fun preloadProgressStartsAtZeroAndAdvancesForSuccessAndFailure() = runTest {
        val progress = mutableListOf<Pair<Int, Int>>()

        val loaded =
            preloadSolarFrameUrls(
                urls = listOf("one", "two", "three"),
                load = { it != "two" },
                onProgress = { completed, total -> progress += completed to total },
            )

        assertEquals(setOf("one", "three"), loaded)
        assertEquals(
            listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
            progress,
        )
    }

    @Test fun eachPreloadInvocationStartsWithFreshProgress() = runTest {
        val first = mutableListOf<Pair<Int, Int>>()
        val second = mutableListOf<Pair<Int, Int>>()

        preloadSolarFrameUrls(
            urls = listOf("one", "two"),
            load = { true },
            onProgress = { completed, total -> first += completed to total },
        )
        preloadSolarFrameUrls(
            urls = listOf("three"),
            load = { true },
            onProgress = { completed, total -> second += completed to total },
        )

        assertEquals(0 to 2, first.first())
        assertEquals(2 to 2, first.last())
        assertEquals(listOf(0 to 1, 1 to 1), second)
    }

    @Test fun emptyPreloadDoesNotEmitZeroOfZeroProgress() = runTest {
        val progress = mutableListOf<Pair<Int, Int>>()

        val loaded =
            preloadSolarFrameUrls(
                urls = emptyList(),
                load = { true },
                onProgress = { completed, total -> progress += completed to total },
            )

        assertEquals(emptySet<String>(), loaded)
        assertEquals(emptyList<Pair<Int, Int>>(), progress)
    }

    @Test fun playbackUsesOnlySuccessfullyPreloadedFrames() {
        assertEquals(emptyList<SolarImage>(), selectPlayableSolarFrames(frames, emptySet()))
        assertEquals(listOf(frames[0], frames[2]), selectPlayableSolarFrames(frames, setOf("one", "three")))
    }

    @Test fun playbackRequiresMultipleDistinctFrameUrls() {
        assertEquals(false, solarHeroCanAnimate(listOf(frames[0])))
        assertEquals(false, solarHeroCanAnimate(listOf(frames[0], frames[0])))
        assertEquals(true, solarHeroCanAnimate(listOf(frames[0], frames[1])))
    }

    @Test fun blendProgressMatchesPwaFrameInterval() {
        assertEquals(0f, solarBlendProgress(0L, 200L), 0f)
        assertEquals(0.5f, solarBlendProgress(100L, 200L), 0f)
        assertEquals(1f, solarBlendProgress(200L, 200L), 0f)
    }
}
