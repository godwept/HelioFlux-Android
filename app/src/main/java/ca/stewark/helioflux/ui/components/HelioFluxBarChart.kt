package ca.stewark.helioflux.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.KpStatus
import ca.stewark.helioflux.core.model.kpStatus
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill

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
    domain: ChartDomain,
    modifier: Modifier = Modifier,
    fixedYDomain: ChartYDomain? = null,
) {
    if (values.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth().height(1.dp))
        return
    }

    val rangeProvider =
        remember(domain, fixedYDomain) {
            CartesianLayerRangeProvider.fixed(
                minX = domain.minX,
                maxX = domain.maxX,
                minY = fixedYDomain?.minY,
                maxY = fixedYDomain?.maxY,
            )
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

    val guideline =
        rememberAxisGuidelineComponent(
            fill = Fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
        )
    val yFormatter =
        remember {
            CartesianValueFormatter { _, value, _ ->
                formatChartValue(value, ChartValueFormat.Compact)
            }
        }
    val xFormatter =
        remember(domain) {
            CartesianValueFormatter { _, value, _ ->
                formatUtcAxisLabel(value, domain)
            }
        }
    val startAxis =
        VerticalAxis.rememberStart(
            valueFormatter = yFormatter,
            guideline = guideline,
        )
    val bottomAxis =
        HorizontalAxis.rememberBottom(
            valueFormatter = xFormatter,
            guideline = guideline,
            itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
        )

    key(domain) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberColumnCartesianLayer(rangeProvider = rangeProvider),
                    startAxis = startAxis,
                    bottomAxis = bottomAxis,
                    getXStep = { chartXStepMillis(domain).toDouble() },
                ),
            modelProducer = modelProducer,
            modifier = modifier,
            scrollState =
                rememberVicoScrollState(
                    scrollEnabled = DefaultChartInteractionPolicy.scrollEnabled,
                ),
            zoomState =
                rememberVicoZoomState(
                    zoomEnabled = DefaultChartInteractionPolicy.zoomEnabled,
                    initialZoom = Zoom.Content,
                ),
        )
    }
}
