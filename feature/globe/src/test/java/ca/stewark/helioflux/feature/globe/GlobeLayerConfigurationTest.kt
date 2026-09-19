package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeLayerConfigurationTest {
    @Test fun shellsSitAboveEarthInLayerOrder() {
        assertEquals(1.0f, EARTH_RADIUS)
        assertTrue(AURORA_RADIUS > EARTH_RADIUS)
        assertTrue(ATMOSPHERE_RADIUS > AURORA_RADIUS)
    }

    @Test fun earthTextureUsesBundledAsset() {
        assertEquals("textures/earth_night.jpg", EARTH_TEXTURE_ASSET)
    }
}
