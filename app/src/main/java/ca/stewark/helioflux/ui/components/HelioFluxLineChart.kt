package ca.stewark.helioflux.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

data class LineChartSeries(
    val points: List<ChartPoint>,
)

data class ChartPoint(
    val x: Double,
    val y: Double?,
)

data class ChartReferenceLine(
    val y: Double,
)

@Composable
fun HelioFluxLineChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
    referenceLines: List<ChartReferenceLine> = emptyList(),
) {
    val drawableSeries = remember(series, referenceLines) {
        val data = series.map { item -> item.points.filter { it.y != null } }
        val xBounds = data.flatten().map { it.x }
        if (xBounds.isEmpty()) {
            data
        } else {
            val minX = xBounds.min()
            val maxX = xBounds.max()
            data + referenceLines.map { reference ->
                listOf(ChartPoint(minX, reference.y), ChartPoint(maxX, reference.y))
            }
        }
    }

    if (drawableSeries.none { it.isNotEmpty() }) {
        Spacer(modifier = modifier.fillMaxWidth().height(1.dp))
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(drawableSeries) {
        modelProducer.runTransaction {
            lineSeries {
                drawableSeries.filter { it.isNotEmpty() }.forEach { points ->
                    series(
                        x = points.map { it.x },
                        y = points.map { requireNotNull(it.y) },
                    )
                }
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
