package ca.stewark.helioflux.feature.alerts

import org.junit.Assert.*
import org.junit.Test

class AlertPolicyTest {
    private val policy = AlertPolicy()

    @Test fun geomagneticPolicyAlertsAtThresholdAndOnlyOnEscalation() {
        assertNull(policy.evaluateKp(4.99, AlertState()))
        assertEquals(AlertDecision.Geomagnetic(5, false), policy.evaluateKp(5.2, AlertState()))
        assertNull(policy.evaluateKp(5.9, AlertState(lastKpBand = 5)))
        assertEquals(AlertDecision.Geomagnetic(6, true), policy.evaluateKp(6.0, AlertState(lastKpBand = 5)))
        assertNull(policy.evaluateKp(5.0, AlertState(lastKpBand = 6)))
    }

    @Test fun flarePolicyOnlyAlertsForNewMAndXFlares() {
        val state = AlertState(notifiedFlareIds = setOf("old"))
        assertNull(policy.evaluateFlare("a", "A9.0", state))
        assertNull(policy.evaluateFlare("b", "B2.0", state))
        assertNull(policy.evaluateFlare("c", "C9.9", state))
        assertNull(policy.evaluateFlare("old", "M1.0", state))
        assertNotNull(policy.evaluateFlare("m", "M1.2", state))
        assertNotNull(policy.evaluateFlare("x", "X2.1", state))
    }
}
