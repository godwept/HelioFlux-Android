package ca.stewark.helioflux.core.data.parser

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarRegionSummaryParserTest {
    private val summary =
        """
        :Product: Solar Region Summary
        :Issued: 2026 Sep 22 0030 UTC
        Joint USAF/NOAA Solar Region Summary
        SRS Number 265 Issued at 0030Z on 22 Sep 2026
        Report compiled from data received at SWO on 21 Sep
        I.  Regions with Sunspots.  Locations Valid at 21/2400Z
        Nmbr Location  Lo  Area  Z   LL   NN Mag Type
        4533 S13E50   003  0040 Hsx  01   01 Alpha
        4534 N11W21   074  0030 Dri  05   10 Beta
        4535 N12E58   355  0030 Hrx  02   01 Alpha
        4536 N03E01   052  0020 Cri  04   07 Beta
        4537 N07W11   063  0010 Bxo  64   03 Beta
        4538 N11W09   062  0010 Bxo  03   03 Beta
        IA. H-alpha Plages without Spots.  Locations Valid at 21/2400Z Sep
        Nmbr  Location  Lo
        4532  S07W68   121
        II. Regions Due to Return 22 Sep to 24 Sep
        Nmbr Lat    Lo
        None
        """.trimIndent()

    @Test
    fun parsesOnlyCurrentSummaryRegionsIncludingPlage() {
        val target = Instant.parse("2026-09-22T13:45:00Z").toEpochMilli()
        val regions = SolarRegionSummaryParser.parse(summary, target)

        assertEquals(
            setOf("4532", "4533", "4534", "4535", "4536", "4537", "4538"),
            regions.map { it.id }.toSet(),
        )
        assertEquals("(4532)", regions.single { it.id == "4532" }.number)
        assertEquals("4538", regions.single { it.id == "4538" }.number)
    }

    @Test
    fun advancesSummaryLocationsToHmibcTimestamp() {
        val nearSummaryTime =
            SolarRegionSummaryParser.parse(
                summary,
                Instant.parse("2026-09-22T00:15:00Z").toEpochMilli(),
            ).associateBy { it.id }
        val currentImage =
            SolarRegionSummaryParser.parse(
                summary,
                Instant.parse("2026-09-22T13:45:00Z").toEpochMilli(),
            ).associateBy { it.id }

        assertTrue(
            currentImage.getValue("4538").helioprojectiveX >
                nearSummaryTime.getValue("4538").helioprojectiveX,
        )
        assertTrue(
            currentImage.getValue("4535").helioprojectiveX >
                nearSummaryTime.getValue("4535").helioprojectiveX,
        )
    }

    @Test
    fun projectedCurrentPositionsMatchExpectedDiskLocations() {
        val target = Instant.parse("2026-09-22T13:45:00Z").toEpochMilli()
        val regions = SolarRegionSummaryParser.parse(summary, target).associateBy { it.id }

        with(regions.getValue("4533")) {
            assertEquals(-654.6, helioprojectiveX, 1.0)
            assertEquals(-311.8, helioprojectiveY, 1.0)
        }
        with(regions.getValue("4535")) {
            assertEquals(-751.5, helioprojectiveX, 1.0)
            assertEquals(129.5, helioprojectiveY, 1.0)
        }
        with(regions.getValue("4538")) {
            assertEquals(283.9, helioprojectiveX, 1.0)
            assertEquals(74.0, helioprojectiveY, 1.0)
        }
    }

    @Test
    fun solarTiltAtCurrentFrameIsAboutSevenDegreesNorth() {
        val target = Instant.parse("2026-09-22T13:45:00Z").toEpochMilli()
        assertEquals(7.05, SolarRegionSummaryParser.solarB0Degrees(target), 0.1)
    }
}
