package ca.stewark.helioflux.core.data.parser

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunspotReportParserTest {
    private val target = Instant.parse("2026-09-22T11:15:00Z").toEpochMilli()

    @Test
    fun choosesClosestReportAndProjectsWestToRightOnHmiDisk() {
        val json =
            """[
                {"time_tag":"2026-09-22T09:00:00","Region":4538,"Location":"N10W10"},
                {"time_tag":"2026-09-22T11:10:00","Region":4538,"Location":"N10W30"},
                {"time_tag":"2026-09-22T12:00:00","Region":4538,"Location":"N10W45"}
            ]"""

        val region = SunspotReportParser.parse(json, target).single()

        assertEquals("4538", region.id)
        assertEquals("4538", region.number)
        assertEquals(492.4, region.helioprojectiveX, 0.5)
        assertEquals(67.6, region.helioprojectiveY, 0.5)
        assertTrue(region.helioprojectiveX > 0.0)
    }

    @Test
    fun eastLocationsProjectLeftAndSouthLocationsProjectDown() {
        val json =
            """[
                {"time_tag":"2026-09-22T11:12:00","Region":4534,"Location":"S05E20"}
            ]"""

        val region = SunspotReportParser.parse(json, target).single()

        assertEquals(-340.7, region.helioprojectiveX, 0.5)
        assertEquals(-201.4, region.helioprojectiveY, 0.5)
    }

    @Test
    fun invalidAndBehindLimbReportsAreIgnored() {
        val json =
            """[
                {"time_tag":"2026-09-22T11:10:00","Region":4538,"Location":"N10W95"},
                {"time_tag":"bad","Region":4537,"Location":"N10W20"},
                {"time_tag":"2026-09-22T11:10:00","Region":0,"Location":"N10W20"},
                {"time_tag":"2026-09-22T11:10:00","Region":4536,"Location":"unknown"}
            ]"""

        assertTrue(SunspotReportParser.parse(json, target).isEmpty())
    }

    @Test
    fun solarTiltForCurrentHmibcDateIsAboutSevenDegreesNorth() {
        assertEquals(7.05, SunspotReportParser.solarB0Degrees(target), 0.1)
    }
}
