package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.geometry.Offset
import ca.stewark.helioflux.ui.components.ChartDomain
import ca.stewark.helioflux.ui.components.ChartReferenceStyle
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SolarActivityPhase10Test {
    @Test
    fun enlilPlayerWrapsAndHandlesNoFrames() {
        var count = 2
        val state = EnlilPlayerState({ count })

        state.advance()
        assertEquals(1, state.index)
        state.advance()
        assertEquals(0, state.index)
        count = 0
        state.advance()
        assertEquals(0, state.index)
    }

    @Test
    fun imageryTransformClampsAndDisablesPanAtUnitScale() {
        val state = ImageryTransformState()

        state.transform(0.5f, Offset(20f, 20f))
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)

        state.transform(10f, Offset(5f, 6f))
        assertEquals(5f, state.scale)
        assertEquals(Offset(5f, 6f), state.offset)

        state.reset()
        assertEquals(1f, state.scale)
    }

    @Test
    fun solarActivityChartWindowRemainsExactly72Hours() {
        val now = 10_000_000L

        assertEquals(72L * 60L * 60L * 1_000L, SolarActivityWindowMillis)
        assertEquals(
            ChartDomain(
                minX = (now - SolarActivityWindowMillis).toDouble(),
                maxX = now.toDouble(),
            ),
            solarActivityChartDomain(now),
        )
    }

    @Test
    fun xrayDomainAndReferenceLinesMatchPlan() {
        assertEquals(1e-9, XrayMinFlux, 0.0)
        assertEquals(1e-2, XrayMaxFlux, 0.0)
        assertEquals(listOf(1e-6, 1e-5, 1e-4), XrayReferenceFluxes)
        assertNull(xrayLogValue(null))
        assertEquals(-9.0, xrayLogValue(1e-12)!!, 0.0001)

        val references = xrayReferenceLines()
        assertEquals(listOf("C", "M", "X"), references.map { it.label })
        assertEquals(
            listOf(
                ChartReferenceStyle.Caution,
                ChartReferenceStyle.Warning,
                ChartReferenceStyle.Alert,
            ),
            references.map { it.style },
        )
    }

    @Test
    fun solarActivityDoesNotGainSpaceWeatherTimeframeSelector() {
        val source =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt")
                .readText()

        assertFalse(source.contains("TimeframeSelector"))
    }

    @Test
    fun flareAndCmePresentationRulesAreStable() {
        assertEquals('X', flareClassGroup("X2.1"))
        assertEquals('A', flareClassGroup(""))
        assertEquals(CmeSpeedEmphasis.Normal, cmeSpeedEmphasis(499.0))
        assertEquals(CmeSpeedEmphasis.Elevated, cmeSpeedEmphasis(500.0))
        assertEquals(CmeSpeedEmphasis.Strong, cmeSpeedEmphasis(1000.0))
        assertEquals("1.0e+03", exponentialAxisLabel(1000.0))
    }

    @Test
    fun eventListsUseRepositoryOrderAndScreenOwnedHeadings() {
        val flareSource =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareList.kt")
                .readText()
        val cmeSource =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/CmeList.kt")
                .readText()

        assertFalse(flareSource.contains("sortedByDescending"))
        assertFalse(flareSource.contains("Recent flares"))
        assertTrue(flareSource.contains("flare-row-"))
        assertFalse(cmeSource.contains("sortedByDescending"))
        assertFalse(cmeSource.contains("Recent CMEs"))
        assertTrue(cmeSource.contains("cme-row-"))
        assertTrue(cmeSource.contains("Details"))
    }

    @Test
    fun fullscreenViewerIsEdgeToEdgeInsteadOfAlertDialog() {
        val source =
            File("src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt")
                .readText()

        assertFalse(source.contains("AlertDialog("))
        assertTrue(source.contains("Dialog("))
        assertTrue(source.contains("usePlatformDefaultWidth = false"))
        assertTrue(source.contains("fillMaxSize()"))
        assertTrue(source.contains("fullscreen-imagery-viewer"))
    }
}
