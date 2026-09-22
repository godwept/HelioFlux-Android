package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class ActiveRegionLabelLayoutTest {
    private val stage = IntSize(400, 400)

    @Test
    fun nonOverlappingLabelsRemainAtAnchors() {
        val labels =
            listOf(
                ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(40, 20)),
                ActiveRegionLabelInput("4533", Offset(300f, 300f), IntSize(40, 20)),
            )

        val placements = resolve(labels)

        assertEquals(2, placements.size)
        placements.forEach { placement ->
            assertEquals(placement.anchor.x, placement.center.x, 0.001f)
            assertEquals(placement.anchor.y, placement.center.y, 0.001f)
            assertNull(placement.leaderEnd)
        }
    }

    @Test
    fun closeLabelsMaintainMinimumGap() {
        val placements =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(36, 16)),
                    ActiveRegionLabelInput("4533", Offset(115f, 100f), IntSize(36, 16)),
                ),
            )

        assertFalse(conflicts(placements[0], placements[1], 10f))
    }

    @Test
    fun placementIsDeterministicForSameLabels() {
        val a = ActiveRegionLabelInput("4532", Offset(200f, 200f), IntSize(36, 16))
        val b = ActiveRegionLabelInput("4533", Offset(204f, 201f), IntSize(36, 16))
        val c = ActiveRegionLabelInput("4534", Offset(201f, 204f), IntSize(36, 16))

        val first = resolve(listOf(a, b, c)).associateBy { it.key }
        val second = resolve(listOf(c, a, b)).associateBy { it.key }

        assertEquals(first.keys, second.keys)
        first.forEach { (key, value) ->
            assertEquals(value.topLeft, second.getValue(key).topLeft)
            assertEquals(value.leaderEnd, second.getValue(key).leaderEnd)
        }
    }

    @Test
    fun edgeAnchorKeepsWholeLabelInsideStage() {
        val placement =
            resolve(
                listOf(ActiveRegionLabelInput("4532", Offset(5f, 5f), IntSize(40, 20))),
            ).single()

        assertTrue(placement.left >= 0f)
        assertTrue(placement.top >= 0f)
        assertTrue(placement.right <= stage.width)
        assertTrue(placement.bottom <= stage.height)
    }

    @Test
    fun placementNeverExceedsMaximumDisplacement() {
        val labels =
            (4532..4536).map {
                ActiveRegionLabelInput(it.toString(), Offset(200f, 200f), IntSize(36, 16))
            }

        val placements = resolve(labels)

        assertEquals(labels.size, placements.size)
        placements.forEach {
            assertTrue(hypot(it.center.x - it.anchor.x, it.center.y - it.anchor.y) <= 48.001f)
        }
    }

    @Test
    fun unavoidableClusterUsesLeastOverlapCandidateWithoutDroppingLabels() {
        val smallStage = IntSize(80, 40)
        val labels =
            listOf(
                ActiveRegionLabelInput("4532", Offset(40f, 20f), IntSize(36, 16)),
                ActiveRegionLabelInput("4533", Offset(40f, 20f), IntSize(36, 16)),
                ActiveRegionLabelInput("4534", Offset(40f, 20f), IntSize(36, 16)),
            )

        val first =
            resolveActiveRegionLabels(labels, smallStage, 10f, 10f, 16f)
        val second =
            resolveActiveRegionLabels(labels.reversed(), smallStage, 10f, 10f, 16f)

        assertEquals(3, first.size)
        first.forEach {
            assertTrue(it.left >= 0f)
            assertTrue(it.top >= 0f)
            assertTrue(it.right <= smallStage.width)
            assertTrue(it.bottom <= smallStage.height)
        }
        assertEquals(
            first.associate { it.key to it.topLeft },
            second.associate { it.key to it.topLeft },
        )
    }

    @Test
    fun displacementAtLeaderThresholdDoesNotUseLeader() {
        val placement =
            resolveActiveRegionLabels(
                listOf(ActiveRegionLabelInput("4532", Offset(0f, 50f), IntSize(32, 12))),
                IntSize(100, 100),
                16f,
                48f,
                16f,
            ).single()

        assertEquals(16f, distance(placement), 0.001f)
        assertNull(placement.leaderEnd)
    }

    @Test
    fun displacementBeyondLeaderThresholdUsesLabelEdge() {
        val placements =
            resolve(
                listOf(
                    ActiveRegionLabelInput("4532", Offset(100f, 100f), IntSize(10, 10)),
                    ActiveRegionLabelInput("4533", Offset(100f, 100f), IntSize(10, 10)),
                ),
            )
        val moved = placements.single { it.key == "4533" }
        val end = requireNotNull(moved.leaderEnd)

        assertTrue(distance(moved) > 16f)
        val onVerticalEdge =
            kotlin.math.abs(end.x - moved.left) < 0.001f ||
                kotlin.math.abs(end.x - moved.right) < 0.001f
        val onHorizontalEdge =
            kotlin.math.abs(end.y - moved.top) < 0.001f ||
                kotlin.math.abs(end.y - moved.bottom) < 0.001f
        assertTrue(onVerticalEdge || onHorizontalEdge)
    }

    private fun resolve(labels: List<ActiveRegionLabelInput>) =
        resolveActiveRegionLabels(labels, stage, 10f, 48f, 16f)

    private fun distance(placement: ActiveRegionLabelPlacement): Float {
        val dx = placement.center.x - placement.anchor.x
        val dy = placement.center.y - placement.anchor.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

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
