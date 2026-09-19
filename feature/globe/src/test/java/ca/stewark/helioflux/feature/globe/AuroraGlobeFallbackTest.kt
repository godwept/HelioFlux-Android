package ca.stewark.helioflux.feature.globe

import ca.stewark.helioflux.core.model.AuroraPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuroraGlobeFallbackTest {
    @Test fun noAuroraPointsStillKeepsEarthRenderable() {
        assertFalse(hasRenderableAurora(emptyList()))
    }

    @Test fun onlyMeaningfulOvationPointsCreateAuroraLayer() {
        assertFalse(hasRenderableAurora(listOf(AuroraPoint(60.0, -50.0, 4.9))))
        assertTrue(hasRenderableAurora(listOf(AuroraPoint(60.0, -50.0, 5.0))))
    }
}

internal fun hasRenderableAurora(points: List<AuroraPoint>) = points.any { it.intensity >= 5.0 }
