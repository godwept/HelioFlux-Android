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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.DataBlue
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.FreshGreen
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.WarningAmber
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

enum class ChartSeriesStyle {
    Default,
    Bz,
    Primary,
    Secondary,
    Alert,
    Purple,
    Success,
    Warning,
}

data class LineChartSeries(
    val points: List<ChartPoint>,
    val style: ChartSeriesStyle = ChartSeriesStyle.Default,
    val label: String = "",
    val valueFormat: ChartValueFormat = ChartValueFormat.Compact,
    val allowReduction: Boolean = true,
)

data class ChartPoint(
    val x: Double,
    val y: Double?,
)

data class ChartReferenceLine(
    val y: Double,
    val label: String? = null,
    val style: ChartReferenceStyle = ChartReferenceStyle.Neutral,
)

fun seriesColor(style: ChartSeriesStyle): Color =
    when (style) {
        ChartSeriesStyle.Default,
        ChartSeriesStyle.Secondary -> DataCyan
        ChartSeriesStyle.Bz,
        ChartSeriesStyle.Alert -> AlertRed
        ChartSeriesStyle.Primary -> DataBlue
        ChartSeriesStyle.Purple -> Color(0xFFBF8CFF)
        ChartSeriesStyle.Success -> FreshGreen
        ChartSeriesStyle.Warning -> WarningAmber
    }

fun referenceColor(style: ChartReferenceStyle): Color =
    when (style) {
        ChartReferenceStyle.Neutral -> SpaceMuted
        ChartReferenceStyle.Caution -> WarningAmber
        ChartReferenceStyle.Warning -> SolarOrange
        ChartReferenceStyle.Alert -> AlertRed
    }

private data class RenderSegment(
    val series: LineChartSeries,
    val points: List<ChartPoint>,
)

@Composable
fun HelioFluxLineChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    domain: ChartDomain? = null,
    fixedYDomain: ChartYDomain? = null,
    yAxisFormat: ChartValueFormat = ChartValueFormat.Compact,
    animateInitial: Boolean = true,
) {
    val resolvedDomain =
        domain
            ?: remember(series) {
                val xValues =
                    series
                        .flatMap(LineChartSeries::points)
                        .map(ChartPoint::x)
                        .filter(Double::isFinite)
                when {
                    xValues.isEmpty() -> ChartDomain(0.0, 1.0)
                    xValues.min() == xValues.max() -> ChartDomain(xValues.min() - 1.0, xValues.max() + 1.0)
                    else -> ChartDomain(xValues.min(), xValues.max())
                }
            }

    val preparedSeries =
        remember(series, resolvedDomain) {
            series.map { prepareLineSeriesForRender(it, resolvedDomain) }
        }
    val renderSegments =
        remember(preparedSeries) {
            preparedSeries.flatMap { item ->
                nonNullSegments(item.points).map { points -> RenderSegment(item, points) }
            }
        }

    if (renderSegments.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth().height(1.dp))
        return
    }

    val resolvedYDomain =
        remember(preparedSeries, resolvedDomain, referenceLines, fixedYDomain) {
            fixedYDomain
                ?: paddedYDomain(
                    values = preparedSeries.flatMap(LineChartSeries::points),
                    domain = resolvedDomain,
                    requiredValues = referenceLines.map(ChartReferenceLine::y),
                )
        }

    val rangeProvider =
        remember(resolvedDomain, resolvedYDomain) {
            CartesianLayerRangeProvider.fixed(
                minX = resolvedDomain.minX,
                maxX = resolvedDomain.maxX,
                minY = resolvedYDomain?.minY,
                maxY = resolvedYDomain?.maxY,
            )
        }

    val lines =
        renderSegments.map { segment ->
            LineCartesianLayer.rememberLine(
                fill =
                    LineCartesianLayer.LineFill.single(
                        Fill(seriesColor(segment.series.style)),
                    ),
            )
        }
    val layer =
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(lines),
            rangeProvider = rangeProvider,
        )

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(renderSegments) {
        modelProducer.runTransaction {
            lineSeries {
                renderSegments.forEach { segment ->
                    series(
                        x = segment.points.map(ChartPoint::x),
                        y = segment.points.map { requireNotNull(it.y) },
                    )
                }
            }
        }
    }

    val guideline =
        rememberAxisGuidelineComponent(
            fill = Fill(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
        )
    val yFormatter =
        remember(yAxisFormat) {
            CartesianValueFormatter { _, value, _ ->
                formatChartValue(value, yAxisFormat)
            }
        }
    val xFormatter =
        remember(resolvedDomain) {
            CartesianValueFormatter { _, value, _ ->
                formatUtcAxisLabel(value, resolvedDomain)
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

    val markerFormatter =
        remember(preparedSeries, resolvedDomain) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val selectedX = targets.firstOrNull()?.x ?: return@ValueFormatter ""
                val rows = markerRowsAtX(preparedSeries, selectedX)
                buildAnnotatedString {
                    append(formatUtcMarkerTimestamp(selectedX))
                    rows.forEach { row ->
                        append("\n")
                        withStyle(SpanStyle(color = seriesColor(row.style))) {
                            append(if (row.label.isBlank()) "Value" else row.label)
                        }
                        append(": ")
                        append(formatChartValue(row.value, row.valueFormat))
                    }
                }
            }
        }
    val marker =
        rememberDefaultCartesianMarker(
            label =
                rememberTextComponent(
                    style =
                        TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                    lineCount = chartMarkerLineCount(preparedSeries.size),
                ),
            valueFormatter = markerFormatter,
            labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
            guideline =
                rememberAxisGuidelineComponent(
                    fill = Fill(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                ),
        )

    val decorations =
        referenceLines.map { reference ->
            HorizontalLine(
                y = { reference.y },
                line =
                    rememberAxisGuidelineComponent(
                        fill = Fill(referenceColor(reference.style).copy(alpha = 0.55f)),
                    ),
                labelComponent =
                    reference.label?.let {
                        rememberTextComponent(
                            style =
                                TextStyle(
                                    color = referenceColor(reference.style),
                                    fontSize = 10.sp,
                                ),
                        )
                    },
                label = { reference.label.orEmpty() },
            )
        }

    val markerController =
        CartesianMarkerController.rememberShowOnPress(
            consumeMoveEvents = DefaultChartInteractionPolicy.consumeMoveEvents,
        )

    key(resolvedDomain) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    layer,
                    startAxis = startAxis,
                    bottomAxis = bottomAxis,
                    marker = marker,
                    decorations = decorations,
                    getXStep = { chartXStepMillis(resolvedDomain).toDouble() },
                    markerController = markerController,
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
            animateIn = animateInitial,
        )
    }
}
