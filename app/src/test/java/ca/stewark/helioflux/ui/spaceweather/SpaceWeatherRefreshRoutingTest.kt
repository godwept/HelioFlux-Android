package ca.stewark.helioflux.ui.spaceweather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpaceWeatherRefreshRoutingTest {
    @Test
    fun refreshableChartsRouteToTheirUnderlyingSources() {
        assertEquals(SpaceWeatherRefreshSource.Magnetic, refreshSourceForBlock(SpaceWeatherBlock.BzBt))
        assertEquals(SpaceWeatherRefreshSource.Plasma, refreshSourceForBlock(SpaceWeatherBlock.Density))
        assertEquals(SpaceWeatherRefreshSource.Plasma, refreshSourceForBlock(SpaceWeatherBlock.Speed))
        assertEquals(SpaceWeatherRefreshSource.Plasma, refreshSourceForBlock(SpaceWeatherBlock.Temperature))
        assertEquals(SpaceWeatherRefreshSource.GoesMagnetometer, refreshSourceForBlock(SpaceWeatherBlock.Goes))
        assertEquals(SpaceWeatherRefreshSource.HemisphericPower, refreshSourceForBlock(SpaceWeatherBlock.HemisphericPower))
    }

    @Test
    fun kpAndNonChartBlocksDoNotGetTargetedRefresh() {
        assertNull(refreshSourceForBlock(SpaceWeatherBlock.Kp))
        assertNull(refreshSourceForBlock(SpaceWeatherBlock.AuroraHero))
        assertNull(refreshSourceForBlock(SpaceWeatherBlock.Timeframe))
    }
}
