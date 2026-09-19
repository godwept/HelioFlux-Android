package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CoreParsersTest {
    @Test fun magneticFormatsAndInvalidTime() {
        val array = """[["time","bx","by","bz","x","y","bt"],["2026-09-19T12:00:00Z","1","2","-3","0","0","4"],["bad","9","9","9","0","0","9"]]"""
        val obj = """[{"time_tag":"2026-09-19T12:00:00","bx_gsm":"1","by_gsm":"2","bz_gsm":"-3","bt":"4"}]"""
        assertEquals(-3.0, MagneticFieldParser.parse(array).single().bz!!, 0.0)
        assertEquals(4.0, MagneticFieldParser.parse(obj).single().bt!!, 0.0)
    }

    @Test fun plasmaFormatsAndZeroRejection() {
        val obj = """[{"time_tag":"2026-09-19T12:00:00","proton_density":"0","proton_speed":"0"},{"time_tag":"2026-09-19T12:01:00","proton_density":"5","proton_speed":"400"},{"time_tag":"bad","proton_density":"5","proton_speed":"400"}]"""
        val array = """[["time","density","speed","temperature"],["2026-09-19T12:02:00Z","6","410","100000"]]"""
        assertEquals(400.0, PlasmaParser.parse(obj).single().speed!!, 0.0)
        assertEquals(6.0, PlasmaParser.parse(array).single().density!!, 0.0)
    }

    @Test fun kpParsingAndThresholds() {
        assertEquals(listOf(KpStatus.Quiet,KpStatus.Unsettled,KpStatus.Minor,KpStatus.Moderate,KpStatus.Strong,KpStatus.Severe), listOf(kpStatus(2.99),kpStatus(3.0),kpStatus(5.0),kpStatus(6.0),kpStatus(7.0),kpStatus(9.0)))
        assertEquals(4.67, KpParser.parse("""[{"time_tag":"2026-09-19T12:00:00","Kp":"4.67"}]""").single().kp!!, 0.0)
    }

    @Test fun goesLabelsMergeAndArcjetGaps() {
        val sources = """[{"magnetometers":{"primary":19,"secondary":18}}]"""
        val primary = """[{"time_tag":"2026-09-19T12:00:00Z","Hp":100,"arcjet_flag":true},{"time_tag":"2026-09-19T12:01:00Z","Hp":101,"arcjet_flag":false}]"""
        val secondary = """[{"time_tag":"2026-09-19T12:00:00Z","Hp":90,"arcjet_flag":false}]"""
        val result = GoesMagnetometerParser.parse(sources, primary, secondary)
        assertEquals("GOES-19", result.primaryLabel)
        assertEquals("GOES-18", result.secondaryLabel)
        assertNull(result.data[0].primary)
        assertEquals(90.0, result.data[0].secondary!!, 0.0)
        assertNull(result.data[1].secondary)
    }

    @Test fun ovationFloorAndThinning() {
        val result = OvationParser.parse("""{"Observation Time":"2026-09-19T12:00:00Z","Forecast Time":"2026-09-19T12:30:00Z","coordinates":[[1,50,4],[1,51,6],[2,52,6],[3,53,9]]}""")!!
        assertEquals(listOf(2.0, 3.0), result.points.map { it.longitude })
    }

    @Test fun hemisphericCommentsShapeAndNumbers() {
        val result = HemisphericPowerParser.parse("# header\n2026-09-19_12:00 x 42.5 37.2\nbad x 1 2\n2026-09-19_12:01 x nope 2")
        assertEquals(1, result.size)
        assertEquals(42.5, result.single().north!!, 0.0)
    }

    @Test fun forecastIssueCommentsSoftLinesAndFourSections() {
        val text = """:Issued: 2026 Sep 19 1200 UTC
# comment
Solar Activity
.24 hr Summary... First
line.
.Forecast... Solar forecast.
Energetic Particle
.24 hr Summary... Particle summary.
.Forecast... Particle forecast.
Solar Wind
.24 hr Summary... Wind summary.
.Forecast... Wind forecast.
Geospace
.24 hr Summary... Geo summary.
.Forecast... Geo forecast."""
        val result = ForecastDiscussionParser.parse(text)
        assertEquals(4, result.size)
        assertEquals("First line.", result[0].summary)
        assertEquals("2026 Sep 19 1200 UTC", result[0].issueTime)
        assertEquals("Geo forecast.", result[3].forecast)
    }
}
