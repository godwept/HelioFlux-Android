package ca.stewark.helioflux.feature.globe

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class EarthTextureAssetTest {
    @Test fun bundledEarthTextureExistsAndIsNotEmpty() {
        val texture = File("src/main/assets/textures/earth_night.jpg")
        assertTrue(texture.isFile)
        texture.inputStream().use { stream ->
            assertTrue(stream.readBytes().isNotEmpty())
        }
    }
}
