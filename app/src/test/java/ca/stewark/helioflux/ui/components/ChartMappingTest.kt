package ca.stewark.helioflux.ui.components

import ca.stewark.helioflux.core.model.KpStatus
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.DataBlue
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.FreshGreen
import ca.stewark.helioflux.ui.theme.WarningAmber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMappingTest {
    @Test
    fun lineSeriesPreservesNullablePointsAndVisualIdentity() {
        val series = LineChartSeries(
            label = "Bz",
            points = listOf(ChartPoint(1.0, 2.0), ChartPoint(2.0, null)),
            style = ChartSeriesStyle.Bz,
            valueFormat = ChartValueFormat.Compact,
            allowReduction = true,
        )
        val reference = ChartReferenceLine(
            y = 5.0,
            label = "C",
            style = ChartReferenceStyle.Caution,
        )

        assertEquals("Bz", series.label)
        assertEquals(2, series.points.size)
        assertEquals(null, series.points.last().y)
        assertEquals(ChartSeriesStyle.Bz, series.style)
        assertEquals(ChartValueFormat.Compact, series.valueFormat)
        assertTrue(series.allowReduction)
        assertEquals(5.0, reference.y, 0.0)
        assertEquals("C", reference.label)
        assertEquals(ChartReferenceStyle.Caution, reference.style)
    }

    @Test
    fun semanticStylesMapToTheExpectedThemeColors() {
        assertEquals(AlertRed, seriesColor(ChartSeriesStyle.Bz))
        assertEquals(DataBlue, seriesColor(ChartSeriesStyle.Primary))
        assertEquals(DataCyan, seriesColor(ChartSeriesStyle.Secondary))
        assertEquals(FreshGreen, seriesColor(ChartSeriesStyle.Success))
        assertEquals(WarningAmber, seriesColor(ChartSeriesStyle.Warning))
    }

    @Test
    fun kpBarsMapValuesToStatusLabelsAndEmptyInputIsSafe() {
        val bars = mapKpBars(listOf(1.0 to 2.0, 2.0 to 5.0, 3.0 to 9.0))

        assertEquals(
            listOf(KpStatus.Quiet, KpStatus.Minor, KpStatus.Severe),
            bars.map { it.status },
        )
        assertTrue(mapKpBars(emptyList()).isEmpty())
    }
}
