package ca.stewark.helioflux.core.data.parser

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveRegionParserTest {
    @Test
    fun choosesObservationClosestToMagnetogramTimestampRegardlessOfResponseOrder() {
        val target = Instant.parse("2026-09-22T11:15:00Z").toEpochMilli()
        val older = """{"ar_noaanum":14538,"hpc_x":10.0,"hpc_y":20.0,"event_starttime":"2026-09-22T09:00:00"}"""
        val closest = """{"ar_noaanum":14538,"hpc_x":50.0,"hpc_y":25.0,"event_starttime":"2026-09-22T11:10:00"}"""
        val later = """{"ar_noaanum":14538,"hpc_x":100.0,"hpc_y":30.0,"event_starttime":"2026-09-22T12:00:00"}"""

        val forward =
            ActiveRegionParser.parse(
                """{"result":[$older,$closest,$later]}""",
                target,
            ).single()
        val reversed =
            ActiveRegionParser.parse(
                """{"result":[$later,$closest,$older]}""",
                target,
            ).single()

        assertEquals(50.0, forward.helioprojectiveX, 0.001)
        assertEquals(25.0, forward.helioprojectiveY, 0.001)
        assertEquals(forward, reversed)
    }

    @Test
    fun timedObservationBeatsUntimedDuplicateWhenTargetIsKnown() {
        val target = Instant.parse("2026-09-22T11:15:00Z").toEpochMilli()
        val timed = """{"ar_noaanum":14538,"hpc_x":50.0,"hpc_y":25.0,"event_starttime":"2026-09-22T11:10:00"}"""
        val untimed = """{"ar_noaanum":14538,"hpc_x":900.0,"hpc_y":25.0}"""

        val region =
            ActiveRegionParser.parse(
                """{"result":[$timed,$untimed]}""",
                target,
            ).single()

        assertEquals(50.0, region.helioprojectiveX, 0.001)
    }
}
