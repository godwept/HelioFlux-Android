package ca.stewark.helioflux.feature.globe

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class AuroraLayerLifecycleTest {
    @Test fun materialBoundAuroraTextureIsNotDestroyedDuringCompositionDisposal() {
        val source = File("src/main/java/ca/stewark/helioflux/feature/globe/AuroraLayer.kt").readText()
        assertFalse(source.contains("safeDestroyTexture(texture)"))
    }
}
