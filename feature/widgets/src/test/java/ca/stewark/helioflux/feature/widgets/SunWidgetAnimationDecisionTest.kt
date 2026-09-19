package ca.stewark.helioflux.feature.widgets

import org.junit.Assert.assertFalse
import org.junit.Test

class SunWidgetAnimationDecisionTest {
    @Test fun unvalidatedAnimationRemainsDisabled() {
        assertFalse(SunWidgetAnimationPolicy.enabled)
    }
}

internal object SunWidgetAnimationPolicy {
    const val enabled = false
}
