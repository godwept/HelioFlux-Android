package ca.stewark.helioflux.ui.solaractivity

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class EnlilPlaybackTest {
    @Test
    fun preloadPolicyUsesApprovedLimits() {
        assertEquals(4, EnlilPreloadConcurrency)
        assertEquals(5, EnlilMaxToleratedFailures)
    }

    @Test
    fun preloadAcceptsUpToFiveFailuresAndPreservesSuccessfulOrder() {
        val result =
            EnlilPreloadResult(
                successfulUrls = listOf("one", "three", "four"),
                failureCount = 5,
            )

        assertTrue(isAcceptedEnlilPreload(result))
        assertEquals(listOf("one", "three", "four"), playableEnlilFrames(result))
    }

    @Test
    fun preloadRejectsSixFailures() {
        val result =
            EnlilPreloadResult(
                successfulUrls = listOf("one"),
                failureCount = 6,
            )

        assertFalse(isAcceptedEnlilPreload(result))
        assertTrue(playableEnlilFrames(result).isEmpty())
    }

    @Test
    fun preloadRejectsNoSuccessfulFrames() {
        val result = EnlilPreloadResult(successfulUrls = emptyList(), failureCount = 1)

        assertFalse(isAcceptedEnlilPreload(result))
        assertTrue(playableEnlilFrames(result).isEmpty())
    }

    @Test
    fun boundedPreloadNeverExceedsFourConcurrentLoads() =
        runTest {
            val urls = (1..12).map { "frame-$it" }
            var active = 0
            var peak = 0

            val result =
                preloadEnlilUrls(urls) {
                    active++
                    peak = maxOf(peak, active)
                    delay(10)
                    active--
                    true
                }

            assertTrue("peak concurrency was $peak", peak <= EnlilPreloadConcurrency)
            assertEquals(urls, result.successfulUrls)
            assertEquals(0, result.failureCount)
        }

    @Test
    fun boundedPreloadWaitsForEveryFrameAndPreservesInputOrder() =
        runTest {
            val urls = listOf("one", "two", "three", "four", "five", "six")
            val completed = mutableListOf<String>()

            val result =
                preloadEnlilUrls(urls) { url ->
                    delay((urls.size - urls.indexOf(url)).toLong())
                    completed += url
                    url !in setOf("two", "five")
                }

            assertEquals(urls.size, completed.size)
            assertEquals(listOf("one", "three", "four", "six"), result.successfulUrls)
            assertEquals(2, result.failureCount)
        }

    @Test
    fun boundedPreloadReportsZeroThenEveryCompletedFrame() =
        runTest {
            val updates = mutableListOf<Pair<Int, Int>>()
            val urls = listOf("one", "two", "three")

            preloadEnlilUrls(
                urls = urls,
                load = { url ->
                    delay(if (url == "one") 20 else 5)
                    url != "two"
                },
                onProgress = { completed, total ->
                    updates += completed to total
                },
            )

            assertEquals(
                listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
                updates,
            )
        }

    @Test
    fun failedFrameStillAdvancesCompletedProgress() =
        runTest {
            val updates = mutableListOf<Pair<Int, Int>>()

            preloadEnlilUrls(
                urls = listOf("one"),
                load = { false },
                onProgress = { completed, total -> updates += completed to total },
            )

            assertEquals(listOf(0 to 1, 1 to 1), updates)
        }

    @Test
    fun cancelledPreloadDoesNotReportCompletedFrames() =
        runTest {
            val updates = mutableListOf<Pair<Int, Int>>()
            val job =
                launch {
                    preloadEnlilUrls(
                        urls = listOf("one", "two"),
                        load = { awaitCancellation() },
                        onProgress = { completed, total -> updates += completed to total },
                    )
                }

            yield()
            job.cancel()
            job.join()

            assertEquals(listOf(0 to 2), updates)
        }

    @Test
    fun enlilProgressFormatsCompletedFrames() {
        assertEquals("Loading frame 18 of 48", formatEnlilProgress(18, 48))
    }

    @Test
    fun playbackStateWaitsForFullPreloadAndHandlesSingleFrame() {
        assertEquals(
            EnlilMediaLoadState.Loading,
            enlilMediaLoadState(listOf("one", "two"), preloadResult = null),
        )

        val single = EnlilPreloadResult(listOf("one"), failureCount = 1)
        assertEquals(EnlilMediaLoadState.Ready, enlilMediaLoadState(listOf("one", "two"), single))
        assertEquals(listOf("one"), playableEnlilFrames(single))
    }

    @Test
    fun playbackStateFailsRejectedPreload() {
        val rejected = EnlilPreloadResult(listOf("one"), failureCount = 6)

        assertEquals(
            EnlilMediaLoadState.Failed,
            enlilMediaLoadState(listOf("one"), rejected),
        )
    }

    @Test
    fun blendProgressRunsContinuouslyAcrossFrameInterval() {
        assertEquals(0f, enlilBlendProgress(0L, 200L), 0f)
        assertEquals(0.5f, enlilBlendProgress(100L, 200L), 0f)
        assertEquals(1f, enlilBlendProgress(200L, 200L), 0f)
    }
}
