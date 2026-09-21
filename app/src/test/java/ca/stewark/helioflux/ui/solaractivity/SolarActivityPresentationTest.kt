package ca.stewark.helioflux.ui.solaractivity

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class SolarActivityPresentationTest {
    private val source = DataSourceKey("test")
    private val sampleUtcMillis = 1789907640000L

    @Test
    fun utcTimestampUsesHumanReadableSolarActivityFormat() {
        assertEquals("Sep 20, 12:34 UTC", formatSolarActivityUtc(sampleUtcMillis))
    }

    @Test
    fun flareProbabilityPresentationUsesApprovedClassesValuesAndAccents() {
        val p =
            flareProbabilityPresentation(
                RepositoryState.Available(
                    FlareProbabilities(c = 60, m = 25, x = 5),
                    source,
                    DataFreshness.Fresh,
                ),
            )

        assertEquals(listOf("C", "M", "X"), p.metrics.map { it.label })
        assertEquals(listOf("60%", "25%", "5%"), p.metrics.map { it.value })
        assertEquals(listOf(WarningAmber, SolarOrange, AlertRed), p.metrics.map { it.accent })
        assertNull(p.message)
    }

    @Test
    fun flareProbabilityPresentationKeepsLoadingFailureAndRetainedDataInline() {
        assertEquals(
            "Loading flare probabilities",
            flareProbabilityPresentation(RepositoryState.Loading).message,
        )
        assertEquals(
            "Flare probabilities unavailable",
            flareProbabilityPresentation(
                RepositoryState.Failure(source, "down", null, null),
            ).message,
        )
        val retained =
            flareProbabilityPresentation(
                RepositoryState.Failure(
                    source,
                    "down",
                    FlareProbabilities(40, 10, 2),
                    DataFreshness.Cached,
                ),
            )
        assertEquals(listOf("40%", "10%", "2%"), retained.metrics.map { it.value })
        assertEquals("Showing cached flare probabilities", retained.message)
    }

    @Test
    fun flarePresentationUsesUtcMetadataAndClassAccents() {
        val p =
            flareEventPresentation(
                FlareEvent(
                    id = "flare",
                    flareClass = "M1.7",
                    timestampMillis = sampleUtcMillis,
                    observatory = "GOES",
                    region = "12345",
                    location = "N12W34",
                ),
            )

        assertEquals("M1.7", p.flareClass)
        assertEquals("Sep 20, 12:34 UTC", p.time)
        assertEquals("GOES · AR 12345 · N12W34", p.metadata)
        assertEquals(SolarOrange, p.accent)
        assertEquals(SpaceMuted, flareClassAccent("A1"))
        assertEquals(FreshGreen, flareClassAccent("B2"))
        assertEquals(DataCyan, flareClassAccent("C3"))
        assertEquals(SolarOrange, flareClassAccent("M4"))
        assertEquals(AlertRed, flareClassAccent("X5"))
    }

    @Test
    fun cmePresentationPreservesSpeedDirectionAndFullWidth() {
        val p =
            cmeEventPresentation(
                CmeEvent(
                    id = "cme",
                    timestampMillis = sampleUtcMillis,
                    speed = 1250.0,
                    halfAngle = 35.0,
                    direction = "NW",
                    type = null,
                    link = "https://example.test/cme",
                ),
            )

        assertEquals("1250 km/s", p.speed)
        assertEquals("Sep 20, 12:34 UTC", p.time)
        assertEquals("NW · 70° wide", p.metadata)
        assertEquals(CmeSpeedEmphasis.Strong, p.emphasis)
    }
}
