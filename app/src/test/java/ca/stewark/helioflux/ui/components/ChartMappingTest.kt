package ca.stewark.helioflux.ui.components

import ca.stewark.helioflux.core.model.KpStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMappingTest {
    @Test
    fun lineSeries_preservesNullablePointsAndReferenceLines() {
        val series = LineChartSeries(listOf(ChartPoint(1.0, 2.0), ChartPoint(2.0, null)))
        val reference = ChartReferenceLine(5.0)

        assertEquals(2, series.points.size)
        assertEquals(null, series.points.last().y)
        assertEquals(5.0, reference.y, 0.0)
    }

    @Test
    fun kpBars_mapValuesToStatusLabels_andEmptyInputIsSafe() {
        val bars = mapKpBars(listOf(1.0 to 2.0, 2.0 to 5.0, 3.0 to 9.0))

        assertEquals(listOf(KpStatus.Quiet, KpStatus.Minor, KpStatus.Severe), bars.map { it.status })
        assertTrue(mapKpBars(emptyList()).isEmpty())
    }
}
