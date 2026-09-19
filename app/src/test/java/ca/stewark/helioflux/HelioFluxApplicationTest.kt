package ca.stewark.helioflux

import ca.stewark.helioflux.feature.alerts.AlertScheduling
import org.junit.Assert.assertEquals
import org.junit.Test

class HelioFluxApplicationTest {
    @Test fun alertInitializationCreatesChannelAndSchedulesOnce() {
        val application = HelioFluxApplication()
        var channelCalls = 0
        val scheduler = object : AlertScheduling {
            var calls = 0
            override fun schedule() { calls++ }
        }

        application.initializeAlerts(channelCreator = { channelCalls++ }, scheduler = scheduler)
        application.initializeAlerts(channelCreator = { channelCalls++ }, scheduler = scheduler)

        assertEquals(1, channelCalls)
        assertEquals(1, scheduler.calls)
    }
}
