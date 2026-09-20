package ca.stewark.helioflux.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChartPresentationTest {
    private val hour = 60L * 60L * 1_000L

    @Test
    fun chartDomainUsesExactRequestedWindow() {
        val now = 10_000_000L

        assertEquals(
            ChartDomain(
                minX = (now - 3L * hour).toDouble(),
                maxX = now.toDouble(),
            ),
            chartDomain(now, 3L * hour),
        )
    }

    @Test
    fun defaultInteractionPolicyDisablesChartNavigation() {
        assertFalse(DefaultChartInteractionPolicy.scrollEnabled)
        assertFalse(DefaultChartInteractionPolicy.zoomEnabled)
        assertFalse(DefaultChartInteractionPolicy.consumeMoveEvents)
    }

    @Test
    fun utcAxisFormattingChangesWithWindowLength() {
        val instant = Instant.parse("2026-09-20T12:34:00Z").toEpochMilli().toDouble()

        assertEquals(
            "12:34",
            formatUtcAxisLabel(instant, ChartDomain(instant - 3L * hour, instant)),
        )
        assertEquals(
            "Sep 20 12:34",
            formatUtcAxisLabel(instant, ChartDomain(instant - 72L * hour, instant)),
        )
    }

    @Test
    fun xrayFormatterConvertsLogValueBackToFlux() {
        assertEquals("1e-06", formatChartValue(-6.0, ChartValueFormat.XrayFlux))
    }

    @Test
    fun xStepMatchesApprovedWindowBands() {
        assertEquals(15L * 60L * 1_000L, chartXStepMillis(ChartDomain(0.0, hour.toDouble())))
        assertEquals(30L * 60L * 1_000L, chartXStepMillis(ChartDomain(0.0, (3L * hour).toDouble())))
        assertEquals(2L * hour, chartXStepMillis(ChartDomain(0.0, (12L * hour).toDouble())))
        assertEquals(12L * hour, chartXStepMillis(ChartDomain(0.0, (48L * hour).toDouble())))
        assertEquals(12L * hour, chartXStepMillis(ChartDomain(0.0, (72L * hour).toDouble())))
    }

    @Test
    fun autoYDomainPadsVisibleValuesWithoutForcingZero() {
        val out = paddedYDomain(
            values = listOf(
                ChartPoint(1.0, 100.0),
                ChartPoint(2.0, 120.0),
            ),
            domain = ChartDomain(0.0, 10.0),
            requiredValues = emptyList(),
        )!!

        assertTrue(out.minY > 0.0)
        assertTrue(out.minY < 100.0)
        assertTrue(out.maxY > 120.0)
    }

    @Test
    fun requiredReferenceValueIsIncludedInYDomain() {
        val out = paddedYDomain(
            values = listOf(ChartPoint(1.0, 5.0), ChartPoint(2.0, 8.0)),
            domain = ChartDomain(0.0, 10.0),
            requiredValues = listOf(0.0),
        )!!

        assertTrue(out.minY <= 0.0)
    }

    @Test
    fun constantSeriesGetsNonZeroRange() {
        val out = paddedYDomain(
            values = listOf(ChartPoint(1.0, 4.0), ChartPoint(2.0, 4.0)),
            domain = ChartDomain(0.0, 10.0),
            requiredValues = emptyList(),
        )!!

        assertTrue(out.maxY > out.minY)
    }

    @Test
    fun sparseSeriesPassesThroughUnchanged() {
        val points = listOf(
            ChartPoint(1.0, 4.0),
            ChartPoint(2.0, 2.0),
            ChartPoint(3.0, 8.0),
        )

        assertEquals(
            points,
            reduceMinMax(points, ChartDomain(0.0, 10.0), maxRenderPoints = 10),
        )
    }

    @Test
    fun denseReductionPreservesBucketExtremaAndRealTimestamps() {
        val points = (0..99).map { x ->
            ChartPoint(x.toDouble(), if (x == 45) -20.0 else x.toDouble())
        }

        val reduced = reduceMinMax(
            points,
            ChartDomain(0.0, 99.0),
            maxRenderPoints = 20,
        )

        assertTrue(reduced.count { it.y != null } <= 20)
        assertTrue(reduced.any { it.x == 45.0 && it.y == -20.0 })
        assertTrue(reduced.filter { it.y != null }.all { candidate -> candidate in points })
        assertEquals(reduced.sortedBy { it.x }, reduced)
    }

    @Test
    fun reductionPreservesAVisibleGapBetweenRuns() {
        val points = listOf(
            ChartPoint(1.0, 1.0),
            ChartPoint(2.0, 2.0),
            ChartPoint(3.0, null),
            ChartPoint(4.0, 3.0),
            ChartPoint(5.0, 4.0),
        )

        val reduced = reduceMinMax(
            points,
            ChartDomain(0.0, 10.0),
            maxRenderPoints = 4,
        )
        val segments = nonNullSegments(reduced)

        assertEquals(2, segments.size)
        assertEquals(listOf(1.0, 2.0), segments[0].map { it.x })
        assertEquals(listOf(4.0, 5.0), segments[1].map { it.x })
    }

    @Test
    fun nonFiniteValuesAreTreatedAsGaps() {
        val segments = nonNullSegments(
            listOf(
                ChartPoint(1.0, 1.0),
                ChartPoint(2.0, Double.NaN),
                ChartPoint(3.0, 3.0),
            ),
        )

        assertEquals(2, segments.size)
    }

    @Test
    fun duplicateTimestampsAreNormalizedDeterministically() {
        val reduced = reduceMinMax(
            listOf(
                ChartPoint(1.0, 2.0),
                ChartPoint(1.0, 3.0),
                ChartPoint(2.0, 4.0),
            ),
            ChartDomain(0.0, 3.0),
            maxRenderPoints = 10,
        )

        assertEquals(listOf(ChartPoint(1.0, 3.0), ChartPoint(2.0, 4.0)), reduced)
    }

    @Test
    fun renderPreparationReducesEligibleSeriesAndKeepsRawSeriesRaw() {
        val dense = (0..999).map { ChartPoint(it.toDouble(), it.toDouble()) }

        val eligible = prepareLineSeriesForRender(
            LineChartSeries(
                label = "Dense",
                points = dense,
                allowReduction = true,
            ),
            ChartDomain(0.0, 999.0),
            maxRenderPoints = 100,
        )
        val raw = prepareLineSeriesForRender(
            LineChartSeries(
                label = "Raw",
                points = dense,
                allowReduction = false,
            ),
            ChartDomain(0.0, 999.0),
            maxRenderPoints = 100,
        )

        assertTrue(eligible.points.count { it.y != null } <= 100)
        assertEquals(1000, raw.points.count { it.y != null })
    }

    @Test
    fun markerRowsUseOnlyRealPlottedValuesAtSelectedTimestamp() {
        val series = listOf(
            LineChartSeries(
                label = "Bz",
                points = listOf(ChartPoint(10.0, -4.0), ChartPoint(20.0, -2.0)),
                style = ChartSeriesStyle.Bz,
            ),
            LineChartSeries(
                label = "Bt",
                points = listOf(ChartPoint(10.0, 6.0), ChartPoint(20.0, null)),
                style = ChartSeriesStyle.Secondary,
            ),
        )

        val rows = markerRowsAtX(series, 20.0)

        assertEquals(listOf("Bz"), rows.map { it.label })
        assertEquals(-2.0, rows.single().value, 0.0)
    }
}
