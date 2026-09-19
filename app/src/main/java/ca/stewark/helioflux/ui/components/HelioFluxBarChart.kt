package ca.stewark.helioflux.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.KpStatus
import ca.stewark.helioflux.core.model.kpStatus
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

data class KpBarPoint(
    val x: Double,
    val value: Double,
    val status: KpStatus,
)

fun mapKpBars(values: List<Pair<Double, Double>>): List<KpBarPoint> =
    values.map { (x, value) -> KpBarPoint(x, value, kpStatus(value)) }

@Composable
fun HelioFluxBarChart(
    values: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth().height(1.dp))
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction {
            columnSeries {
                series(
                    x = values.map { it.first },
                    y = values.map { it.second },
                )
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(rememberColumnCartesianLayer()),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
