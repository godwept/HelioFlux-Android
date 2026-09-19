package ca.stewark.helioflux.feature.globe

import android.graphics.Color
import ca.stewark.helioflux.core.model.AuroraPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuroraTextureGeneratorTest {
    @Test fun generatesTransparentAndColoredRepresentativePixels() {
        val bitmap = AuroraTextureGenerator.generate(
            listOf(
                AuroraPoint(latitude = 0.0, longitude = 0.0, intensity = 16.0),
                AuroraPoint(latitude = 45.0, longitude = 90.0, intensity = 4.0),
            ),
            width = 36,
            height = 18,
        )

        val center = bitmap.getPixel(18, 9)
        assertEquals(52, Color.red(center))
        assertEquals(199, Color.green(center))
        assertEquals(89, Color.blue(center))
        assertTrue(Color.alpha(center) > 0)
        assertEquals(0, Color.alpha(bitmap.getPixel(27, 4)))
    }
}
