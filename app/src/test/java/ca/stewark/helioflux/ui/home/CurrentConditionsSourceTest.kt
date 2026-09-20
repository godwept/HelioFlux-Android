package ca.stewark.helioflux.ui.home

import java.io.File
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
}
