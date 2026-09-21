package ca.stewark.helioflux.ui.solaractivity

import ca.stewark.helioflux.core.model.AceEpamSample
import ca.stewark.helioflux.ui.components.ChartSeriesStyle
import ca.stewark.helioflux.ui.components.ChartValueFormat
import ca.stewark.helioflux.ui.components.ChartYDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AceEpamChartTest {
    @Test
    fun `particle environment matches NOAA five proton channel product`() {
        val samples =
            listOf(
                AceEpamSample(
                    timestampMillis = 1L,
                    electronLow = 9999.0,
                    electronHigh = 8888.0,
                    protonLow = 10_000.0,
                    protonMid = 100.0,
                    protonHigh = 1.0,
                    protonP7 = 0.1,
                    protonP8 = 0.01,
                ),
            )

        val series = aceEpamSeries(samples)

        assertEquals(
            listOf(
                "Proton 47-68 keV",
                "Proton 115-195 keV",
                "Proton 310-580 keV",
                "Proton 795-1193 keV",
                "Proton 1060-1900 keV",
            ),
            series.map { it.label },
        )
        assertEquals(
            listOf(
                ChartSeriesStyle.Alert,
                ChartSeriesStyle.Warning,
                ChartSeriesStyle.Primary,
                ChartSeriesStyle.Secondary,
                ChartSeriesStyle.Success,
            ),
            series.map { it.style },
        )
        assertEquals(List(5) { ChartValueFormat.LogScientific }, series.map { it.valueFormat })
        assertEquals(listOf(4.0, 2.0, 0.0, -1.0, -2.0), series.map { it.points.single().y })
    }

    @Test
    fun `particle log scale matches NOAA visible range and preserves missing values`() {
        assertEquals(1e-2, EpamMinFlux, 0.0)
        assertEquals(1e6, EpamMaxFlux, 0.0)
        assertEquals(ChartYDomain(-2.0, 6.0), EpamYDomain)
        assertNull(epamLogValue(null))
        assertNull(epamLogValue(0.0))
        assertEquals(-2.0, epamLogValue(1e-6)!!, 0.0001)
        assertEquals(6.0, epamLogValue(1e9)!!, 0.0001)
    }
}
