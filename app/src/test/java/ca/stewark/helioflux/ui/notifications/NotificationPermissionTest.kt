package ca.stewark.helioflux.ui.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {
    @Test fun preAndroid13NeverRequestsRuntimePermission() {
        assertFalse(NotificationPermission.shouldRequest(32, alertsIntroduced = true, alreadyGranted = false))
    }

    @Test fun android13PlusRequestsOnlyWhenIntroducedAndMissing() {
        assertTrue(NotificationPermission.shouldRequest(33, alertsIntroduced = true, alreadyGranted = false))
        assertFalse(NotificationPermission.shouldRequest(33, alertsIntroduced = false, alreadyGranted = false))
        assertFalse(NotificationPermission.shouldRequest(35, alertsIntroduced = true, alreadyGranted = true))
    }
}
