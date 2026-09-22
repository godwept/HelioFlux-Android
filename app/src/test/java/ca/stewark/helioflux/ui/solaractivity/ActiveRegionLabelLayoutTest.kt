package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class ActiveRegionLabelLayoutTest {
    private val stage = IntSize(400, 400)

    @Test
    fun defaultPlacementSitsAboveAnchorAndNeverCoversSunspot() {
        val placement =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(40, 20)),
                ),
            ).single()

        assertEquals(100f, placement.center.x, 0.001f)
        assertEquals(74f, placement.top, 0.001f)
        assertEquals(94f, placement.bottom, 0.001f)
        assertFalse(placement.contains(placement.anchor))
        assertNull(placement.leaderEnd)
    }

    @Test
    fun collisionUsesBelowBeforeHorizontalNudge() {
        val placements =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(40, 20)),
                    ActiveRegionLabelInput("4533", Offset(100f, 100f), IntSize(40, 20)),
                ),
            ).associateBy { it.key }

        val first = placements.getValue("4532")
        val second = placements.getValue("4533")
        assertEquals(74f, first.top, 0.001f)
        assertEquals(106f, second.top, 0.001f)
        assertEquals(100f, second.center.x, 0.001f)
        assertFalse(second.contains(second.anchor))
        assertNull(second.leaderEnd)
    }

    @Test
    fun tightClusterUsesSmallHorizontalNudgeAndLeader() {
        val placements =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(10, 10)),
                    ActiveRegionLabelInput("4533", Offset(100f, 100f), IntSize(10, 10)),
                    ActiveRegionLabelInput("4534", Offset(100f, 100f), IntSize(10, 10)),
                ),
            )
        val moved = placements.single { it.key == "4534" }

        assertTrue(abs(moved.center.x - moved.anchor.x) > 0.001f)
        assertTrue(abs(moved.center.x - moved.anchor.x) <= 24.001f)
        assertFalse(moved.contains(moved.anchor))
        assertNotNull(moved.leaderEnd)
        assertTrue(moved.leaderEnd!!.liesOnEdgeOf(moved))
    }

    @Test
    fun labelsMaintainMinimumGapWhenSpaceExists() {
        val placements =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(10, 10)),
                    ActiveRegionLabelInput("4533", Offset(100f, 100f), IntSize(10, 10)),
                    ActiveRegionLabelInput("4534", Offset(100f, 100f), IntSize(10, 10)),
                ),
            )

        for (i in placements.indices) {
            for (j in i + 1 until placements.size) {
                assertFalse(conflicts(placements[i], placements[j], 10f))
            }
        }
    }

    @Test
    fun edgeAnchorMovesInwardButStaysWithinHorizontalCap() {
        val placement =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(5f, 5f), IntSize(40, 20)),
                ),
            ).single()

        assertTrue(placement.left >= 0f)
        assertTrue(placement.top >= 0f)
        assertTrue(placement.right <= stage.width)
        assertTrue(placement.bottom <= stage.height)
        assertTrue(abs(placement.center.x - placement.anchor.x) <= 24.001f)
        assertFalse(placement.contains(placement.anchor))
        assertNotNull(placement.leaderEnd)
    }

    @Test
    fun crowdedLabelsNeverDriftBeyondHorizontalLimit() {
        val placements =
            resolve(
                (4532..4537).map {
                    ActiveRegionLabelInput(it.toString(), Offset(200f, 200f), IntSize(36, 16))
                },
            )

        assertEquals(6, placements.size)
        placements.forEach {
            assertTrue(abs(it.center.x - it.anchor.x) <= 24.001f)
            assertFalse(it.contains(it.anchor))
        }
    }

    @Test
    fun placementIsDeterministicForSameLabels() {
        val labels =
            listOf(
                ActiveRegionLabelInput("4532", Offset(200f, 200f), IntSize(36, 16)),
                ActiveRegionLabelInput("4533", Offset(204f, 201f), IntSize(36, 16)),
                ActiveRegionLabelInput("4534", Offset(201f, 204f), IntSize(36, 16)),
            )

        val first = resolve(labels).associateBy { it.key }
        val second = resolve(labels.reversed()).associateBy { it.key }

        assertEquals(first.keys, second.keys)
        first.forEach { (key, value) ->
            assertEquals(value.topLeft, second.getValue(key).topLeft)
            assertEquals(value.leaderEnd, second.getValue(key).leaderEnd)
        }
    }

    private fun resolve(labels: List<ActiveRegionLabelInput>) =
        resolveActiveRegionLabels(
            labels = labels,
            stageSize = stage,
            minLabelGapPx = 10f,
            anchorGapPx = 6f,
            maxHorizontalNudgePx = 24f,
            horizontalNudgeStepPx = 8f,
        )

    private fun conflicts(
        a: ActiveRegionLabelPlacement,
        b: ActiveRegionLabelPlacement,
        gap: Float,
    ): Boolean =
        !(
            a.right + gap <= b.left ||
                b.right + gap <= a.left ||
                a.bottom + gap <= b.top ||
                b.bottom + gap <= a.top
        )
}

private val ActiveRegionLabelPlacement.center: Offset
    get() = Offset(left + size.width / 2f, top + size.height / 2f)

private val ActiveRegionLabelPlacement.left: Float
    get() = topLeft.x

private val ActiveRegionLabelPlacement.top: Float
    get() = topLeft.y

private val ActiveRegionLabelPlacement.right: Float
    get() = topLeft.x + size.width

private val ActiveRegionLabelPlacement.bottom: Float
    get() = topLeft.y + size.height

private fun ActiveRegionLabelPlacement.contains(point: Offset): Boolean =
    point.x >= left && point.x <= right && point.y >= top && point.y <= bottom

private fun Offset.liesOnEdgeOf(placement: ActiveRegionLabelPlacement): Boolean =
    abs(x - placement.left) < 0.001f ||
        abs(x - placement.right) < 0.001f ||
        abs(y - placement.top) < 0.001f ||
        abs(y - placement.bottom) < 0.001f
