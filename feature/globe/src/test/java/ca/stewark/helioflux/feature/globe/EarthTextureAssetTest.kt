package ca.stewark.helioflux.feature.globe

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EarthTextureAssetTest {
    @Test fun bundledEarthTextureExistsAndIsNotEmpty() {
        val assets = RuntimeEnvironment.getApplication().assets
        assets.open("textures/earth_night.jpg").use { stream ->
            assertTrue(stream.readBytes().isNotEmpty())
        }
    }
}
