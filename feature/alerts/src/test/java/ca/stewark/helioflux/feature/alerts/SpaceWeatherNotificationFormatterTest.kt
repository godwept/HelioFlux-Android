package ca.stewark.helioflux.feature.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceWeatherNotificationFormatterTest {
    private val formatter = SpaceWeatherNotificationFormatter()

    @Test fun formatsGeomagneticInitialAndEscalation() {
        assertEquals(NotificationText("Geomagnetic storm watch", "Kp has reached 5."), formatter.geomagnetic(5, false))
        assertEquals(NotificationText("Geomagnetic storm intensifying", "Kp has escalated to 7."), formatter.geomagnetic(7, true))
    }

    @Test fun formatsMAndXFlaresConcisely() {
        assertEquals(NotificationText("Solar flare", "M2.4-class flare detected."), formatter.flare("M2.4"))
        assertEquals(NotificationText("Major solar flare", "X1.1-class flare detected."), formatter.flare("X1.1"))
    }
}
