package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.GoesMagnetometerSeries
import ca.stewark.helioflux.core.model.*
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.ui.components.*
import org.junit.Assert.*
import org.junit.Test

class SpaceWeatherChartsTest {
    private val now = 10_000_000L

    @Test
    fun spaceWeatherDomainUsesSelectedTimeframeInsteadOfSampleBounds() {
        assertEquals(
            ChartDomain(
                minX = (now - Timeframe.TwoDays.durationMillis).toDouble(),
                maxX = now.toDouble(),
            ),
            spaceWeatherChartDomain(Timeframe.TwoDays, now),
        )
    }

    @Test
    fun spaceWeatherChartHeightMatchesApprovedPixelPresentation() {
        assertEquals(240.dp, SpaceWeatherChartHeight)
    }

    @Test
    fun chartRefreshControlUsesApprovedPixelDimensions() {
        assertEquals(48.dp, ChartRefreshTouchTarget)
        assertEquals(40.dp, ChartRefreshVisibleSize)
        assertEquals(28.dp, ChartRefreshIconSize)
    }

    @Test
    fun bzBtUsesFilteredDataAndZeroReference() {
        val samples = listOf(
            SolarWindMag(now - 4 * 60 * 60 * 1000, null, null, -9.0, 10.0),
            SolarWindMag(now - 1000, null, null, -2.0, 4.0),
        )
        val out = bzBtSeries(samples, Timeframe.OneHour, now)

        assertEquals(listOf(-2.0), out[0].points.map { it.y })
        assertEquals(listOf("Bz", "Bt"), out.map { it.label })
        assertEquals(
            listOf(ChartSeriesStyle.Bz, ChartSeriesStyle.Secondary),
            out.map { it.style },
        )
        assertEquals(0.0, ChartReferenceLine(0.0).y, 0.0)
    }

    @Test
    fun plasmaMetricsCanUseDistinctSemanticStyles() {
        val samples = listOf(SolarWindPlasma(now - 1, 5.0, 400.0, 90_000.0))
        assertEquals(ChartSeriesStyle.Warning, plasmaSeries(samples, Timeframe.OneHour, now, "Density", ChartSeriesStyle.Warning) { it.density }.single().style)
        assertEquals(ChartSeriesStyle.Success, plasmaSeries(samples, Timeframe.OneHour, now, "Speed", ChartSeriesStyle.Success) { it.speed }.single().style)
        assertEquals(ChartSeriesStyle.Primary, plasmaSeries(samples, Timeframe.OneHour, now, "Temperature", ChartSeriesStyle.Primary) { it.temperature }.single().style)
    }

    @Test
    fun hemisphericPowerShowsAllAvailableSamples() {
        val oldest = HemisphericPowerSample(now - 36 * 60 * 60 * 1000L, 40.0, 30.0)
        val newest = HemisphericPowerSample(now - 5 * 60 * 1000L, 55.0, 45.0)
        val out = hemisphericPowerSeries(listOf(oldest, newest))

        assertEquals(
            listOf(oldest.timestampMillis.toDouble(), newest.timestampMillis.toDouble()),
            out[0].points.map { it.x },
        )
        assertEquals(listOf(40.0, 55.0), out[0].points.map { it.y })
        assertEquals(listOf(30.0, 45.0), out[1].points.map { it.y })
    }

    @Test
    fun plasmaFieldsKeepMissingValuesNull() {
        val samples = listOf(SolarWindPlasma(now - 1, 5.0, null, 90000.0))

        assertNull(
            plasmaSeries(samples, Timeframe.OneHour, now, label = "Speed") { it.speed }
                .single()
                .points
                .single()
                .y,
        )
        assertEquals(
            5.0,
            plasmaSeries(samples, Timeframe.OneHour, now, label = "Density") { it.density }
                .single()
                .points
                .single()
                .y!!,
            0.0,
        )
        assertEquals(
            "Temperature",
            plasmaSeries(samples, Timeframe.OneHour, now, label = "Temperature") { it.temperature }
                .single()
                .label,
        )
    }

    @Test
    fun goesKeepsDynamicLabelsAndNullGaps() {
        val goes = GoesMagnetometerSeries(
            listOf(
                GoesMagSample(now - 2, 1.0, 2.0),
                GoesMagSample(now - 1, null, 3.0),
            ),
            "P",
            "S",
        )
        val out = goesSeries(goes, Timeframe.OneHour, now)

        assertEquals(listOf("P", "S"), out.map { it.label })
        assertEquals(
            listOf(ChartSeriesStyle.Primary, ChartSeriesStyle.Alert),
            out.map { it.style },
        )
        assertNull(out[0].points.last().y)
    }

    @Test
    fun chartMetadataMatchesApprovedHierarchy() {
        assertEquals(
            ChartCardMeta("Magnetic Field", "IMF Bz / Bt (nT)", listOf("Bz", "Bt")),
            bzBtChartMeta,
        )
        assertEquals("Solar Wind", densityChartMeta.context)
        assertEquals("Density (p/cm³)", densityChartMeta.title)
        assertEquals("Solar Wind", speedChartMeta.context)
        assertEquals("Speed (km/s)", speedChartMeta.title)
        assertEquals("Solar Wind", temperatureChartMeta.context)
        assertEquals("Temperature (K)", temperatureChartMeta.title)
        assertEquals(
            ChartCardMeta(
                "GOES Magnetometer",
                "Magnetic Field (nT)",
                listOf("GOES-19", "GOES-18"),
            ),
            goesChartMeta("GOES-19", "GOES-18"),
        )
        assertEquals(emptyList<String>(), goesChartMeta(null, null).legendLabels)
    }

    @Test
    fun hemisphericPowerHasNorthAndSouthRawSeries() {
        val out = hemisphericPowerSeries(
            listOf(HemisphericPowerSample(now - 1, 40.0, 30.0)),
        )

        assertEquals(40.0, out[0].points.single().y!!, 0.0)
        assertEquals(30.0, out[1].points.single().y!!, 0.0)
        assertEquals(listOf("North", "South"), out.map { it.label })
        assertTrue(out.none { it.allowReduction })
        assertEquals(listOf("North", "South"), hemisphericPowerMeta.legendLabels)
    }

    @Test
    fun missingGoesDoesNotFabricateSeries() {
        assertTrue(goesSeriesOrEmpty(null, Timeframe.OneHour, now).isEmpty())
    }

    @Test
    fun hemisphericEmptyStateContractIsStable() {
        assertEquals("No hemispheric power data", hemisphericPowerEmptyMessage)
    }
}
