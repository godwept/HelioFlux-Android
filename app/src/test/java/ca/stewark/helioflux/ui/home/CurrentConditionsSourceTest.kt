package ca.stewark.helioflux.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentConditionsSourceTest {
    @Test
    fun metricPillCentersLabelAndValueGroup() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt",
        ).readText()
        val metric = source.substringAfter("private fun Metric(")

        assertTrue(
            metric.contains(
                "Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)",
            ),
        )
        assertTrue(
            metric.contains(
                "horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)",
            ),
        )
    }

    @Test
    fun currentConditionsShowsOnlyPillsWithApprovedVerticalPadding() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt",
        ).readText()

        assertTrue(source.contains(".padding(vertical = 12.dp)"))
        assertFalse(source.contains("HelioFluxSectionHeading(\"Current Conditions\""))
        assertFalse(source.contains("Aurora / geomagnetic status unavailable"))
        assertFalse(source.contains("Geomagnetic storm conditions"))
        assertFalse(source.contains("Geomagnetic conditions below storm level"))
        assertTrue(source.contains("Metric(\"Kp\""))
        assertTrue(source.contains("Metric(\"Bz\""))
        assertTrue(source.contains("Metric(\"Wind\""))
    }
}
