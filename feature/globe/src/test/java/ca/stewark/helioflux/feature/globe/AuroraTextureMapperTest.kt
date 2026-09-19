package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertEquals
import org.junit.Test

class AuroraTextureMapperTest {
    @Test fun longitudeWrapsAcrossEquirectangularTexture() {
        assertEquals(TexturePixel(0, 50), AuroraTextureMapper.map(0.0, -180.0, 100, 100))
        assertEquals(TexturePixel(50, 50), AuroraTextureMapper.map(0.0, 0.0, 100, 100))
        assertEquals(TexturePixel(0, 50), AuroraTextureMapper.map(0.0, 180.0, 100, 100))
    }

    @Test fun latitudeClampsFromNorthToSouth() {
        assertEquals(0, AuroraTextureMapper.map(90.0, 0.0, 100, 100).y)
        assertEquals(50, AuroraTextureMapper.map(0.0, 0.0, 100, 100).y)
        assertEquals(99, AuroraTextureMapper.map(-90.0, 0.0, 100, 100).y)
        assertEquals(0, AuroraTextureMapper.map(120.0, 0.0, 100, 100).y)
        assertEquals(99, AuroraTextureMapper.map(-120.0, 0.0, 100, 100).y)
    }
}
