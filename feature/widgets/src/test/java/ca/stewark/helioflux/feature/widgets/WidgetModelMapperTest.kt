package ca.stewark.helioflux.feature.widgets

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetModelMapperTest {
    private val source = DataSourceKey("NOAA")

    @Test
    fun cachedRepositoryStateMapsToCompactDisplayStringsAndCachedIndicator() {
        val model = WidgetModelMapper.spaceWeather(
            kp = available(listOf(KpSample(1, 5.67))),
            magnetic = available(listOf(SolarWindMag(1, null, null, -7.25, null))),
            plasma = available(listOf(SolarWindPlasma(1, null, 512.4, null))),
            flare = available(FlareProbabilities(45, 18, 2)),
        )

        assertEquals("Kp 5.7", model.kp)
        assertEquals("Bz -7.3 nT", model.bz)
        assertEquals("512 km/s", model.speed)
        assertEquals("M 18% · X 2%", model.flareStatus)
        assertEquals(DataFreshness.Cached, model.freshness)
    }

    @Test
    fun delayedSourceMakesCombinedWidgetDelayedWhileFreshSourcesStayFresh() {
        val fresh = RepositoryState.Available(listOf(KpSample(1, 2.0)), source, DataFreshness.Fresh)
        val delayed = RepositoryState.Available(AuroraSnapshot(1, 2, emptyList()), source, DataFreshness.Delayed)
        val power = RepositoryState.Available(listOf(HemisphericPowerSample(1, 32.0, 28.0)), source, DataFreshness.Fresh)

        val model = WidgetModelMapper.aurora(fresh, delayed, power)

        assertEquals(DataFreshness.Delayed, model.freshness)
        assertEquals("North 32 GW", model.hemisphericPower)
    }

    private fun <T> available(data: T) = RepositoryState.Available(data, source, DataFreshness.Cached)
}
