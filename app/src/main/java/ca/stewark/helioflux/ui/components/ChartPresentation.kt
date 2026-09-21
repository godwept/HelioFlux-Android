package ca.stewark.helioflux.ui.components

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

data class ChartDomain(
    val minX: Double,
    val maxX: Double,
)

fun chartDomain(nowMillis: Long, durationMillis: Long): ChartDomain =
    ChartDomain(
        minX = (nowMillis - durationMillis).toDouble(),
        maxX = nowMillis.toDouble(),
    )

data class ChartInteractionPolicy(
    val scrollEnabled: Boolean,
    val zoomEnabled: Boolean,
    val consumeMoveEvents: Boolean,
)

val DefaultChartInteractionPolicy = ChartInteractionPolicy(
    scrollEnabled = false,
    zoomEnabled = false,
    consumeMoveEvents = false,
)

enum class ChartValueFormat {
    Compact,
    Scientific,
    LogScientific,
    XrayFlux,
}

data class ChartYDomain(
    val minY: Double,
    val maxY: Double,
)

enum class ChartReferenceStyle {
    Neutral,
    Caution,
    Warning,
    Alert,
}

data class ChartMarkerRow(
    val label: String,
    val value: Double,
    val style: ChartSeriesStyle,
    val valueFormat: ChartValueFormat,
)

const val ChartYRangePaddingFraction = 0.08
const val DefaultMaxRenderPoints = 512

private const val HourMillis = 60L * 60L * 1_000L
private val ShortUtcFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.US).withZone(ZoneOffset.UTC)
private val LongUtcFormatter =
    DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.US).withZone(ZoneOffset.UTC)
private val MarkerUtcDateFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneOffset.UTC)
private val MarkerUtcTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm 'UTC'", Locale.US).withZone(ZoneOffset.UTC)

fun formatUtcAxisLabel(xMillis: Double, domain: ChartDomain): String {
    val formatter =
        if (domain.maxX - domain.minX <= 12L * HourMillis) {
            ShortUtcFormatter
        } else {
            LongUtcFormatter
        }
    return formatter.format(Instant.ofEpochMilli(xMillis.toLong()))
}

fun formatUtcMarkerTimestamp(xMillis: Double): String {
    val instant = Instant.ofEpochMilli(xMillis.toLong())
    return MarkerUtcDateFormatter.format(instant) + "\n" + MarkerUtcTimeFormatter.format(instant)
}

fun chartMarkerLineCount(seriesCount: Int): Int =
    2 + seriesCount.coerceAtLeast(0)

fun chartXStepMillis(domain: ChartDomain): Long {
    val duration = (domain.maxX - domain.minX).coerceAtLeast(0.0)
    return when {
        duration <= HourMillis -> 15L * 60L * 1_000L
        duration <= 3L * HourMillis -> 30L * 60L * 1_000L
        duration <= 12L * HourMillis -> 2L * HourMillis
        duration <= 48L * HourMillis -> 12L * HourMillis
        else -> 12L * HourMillis
    }
}

fun formatChartValue(value: Double, format: ChartValueFormat): String =
    when (format) {
        ChartValueFormat.Compact -> formatCompact(value)
        ChartValueFormat.Scientific -> String.format(Locale.US, "%.1e", value)
        ChartValueFormat.LogScientific -> String.format(Locale.US, "%.1e", 10.0.pow(value))
        ChartValueFormat.XrayFlux -> String.format(Locale.US, "%.0e", 10.0.pow(value))
    }

private fun formatCompact(value: Double): String {
    if (!value.isFinite()) return "—"
    val magnitude = abs(value)
    return when {
        magnitude >= 1_000_000.0 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        magnitude >= 1_000.0 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
        magnitude >= 100.0 -> String.format(Locale.US, "%.0f", value)
        magnitude >= 10.0 -> String.format(Locale.US, "%.1f", value)
        magnitude >= 1.0 -> String.format(Locale.US, "%.2f", value)
        magnitude == 0.0 -> "0"
        magnitude < 0.01 -> String.format(Locale.US, "%.1e", value)
        else -> String.format(Locale.US, "%.2f", value)
    }.trimDecimalZeros()
}

private fun String.trimDecimalZeros(): String {
    val suffixIndex = indexOfFirst { it == 'k' || it == 'M' }
    val numeric = if (suffixIndex >= 0) substring(0, suffixIndex) else this
    val suffix = if (suffixIndex >= 0) substring(suffixIndex) else ""
    val trimmed =
        if (numeric.contains('.') && !numeric.contains('e')) {
            numeric.trimEnd('0').trimEnd('.')
        } else {
            numeric
        }
    return trimmed + suffix
}

fun paddedYDomain(
    values: List<ChartPoint>,
    domain: ChartDomain,
    requiredValues: List<Double>,
): ChartYDomain? {
    val visible =
        values
            .asSequence()
            .filter { it.x.isFinite() && it.x >= domain.minX && it.x <= domain.maxX }
            .mapNotNull { it.y?.takeIf(Double::isFinite) }
            .toMutableList()
    visible += requiredValues.filter(Double::isFinite)
    if (visible.isEmpty()) return null

    val minY = visible.min()
    val maxY = visible.max()
    val padding =
        if (minY == maxY) {
            max(abs(minY) * ChartYRangePaddingFraction, 1.0)
        } else {
            (maxY - minY) * ChartYRangePaddingFraction
        }
    return ChartYDomain(minY - padding, maxY + padding)
}

private fun normalizeChartPoints(
    points: List<ChartPoint>,
    domain: ChartDomain,
): List<ChartPoint> {
    val byX = linkedMapOf<Double, ChartPoint>()
    points
        .asSequence()
        .filter { it.x.isFinite() && it.x >= domain.minX && it.x <= domain.maxX }
        .sortedBy { it.x }
        .forEach { point ->
            byX[point.x] =
                if (point.y == null || !point.y.isFinite()) {
                    ChartPoint(point.x, null)
                } else {
                    point
                }
        }
    return collapseGapMarkers(byX.values.toList())
}

private fun collapseGapMarkers(points: List<ChartPoint>): List<ChartPoint> {
    val result = ArrayList<ChartPoint>(points.size)
    points.forEach { point ->
        if (point.y == null && result.lastOrNull()?.y == null) return@forEach
        result += point
    }
    return result
}

fun nonNullSegments(points: List<ChartPoint>): List<List<ChartPoint>> {
    val segments = mutableListOf<MutableList<ChartPoint>>()
    var current: MutableList<ChartPoint>? = null
    points.forEach { point ->
        val y = point.y
        if (y == null || !y.isFinite()) {
            current = null
        } else {
            if (current == null) {
                current = mutableListOf()
                segments += current!!
            }
            current!!.add(point)
        }
    }
    return segments
}

fun reduceMinMax(
    points: List<ChartPoint>,
    domain: ChartDomain,
    maxRenderPoints: Int = DefaultMaxRenderPoints,
): List<ChartPoint> {
    val normalized = normalizeChartPoints(points, domain)
    val valid = normalized.filter { it.y != null }
    if (valid.isEmpty()) return normalized
    if (valid.size <= maxRenderPoints.coerceAtLeast(1)) return normalized

    val budget = maxRenderPoints.coerceAtLeast(1)
    val first = valid.first()
    val last = valid.last()
    if (budget == 1) return listOf(first)
    if (budget == 2) return listOf(first, last).distinctBy { it.x }

    if (budget == 3) {
        val extreme = valid.minBy { requireNotNull(it.y) }
        return insertGapMarkers(
            normalized,
            listOf(first, extreme, last).distinctBy { it.x }.sortedBy { it.x },
        )
    }

    val domainLength = domain.maxX - domain.minX
    if (!domainLength.isFinite() || domainLength <= 0.0) {
        return listOf(first, last).distinctBy { it.x }
    }

    val bucketCount = ((budget - 2) / 2).coerceAtLeast(1)
    val buckets = Array(bucketCount) { mutableListOf<ChartPoint>() }
    valid.forEach { point ->
        val fraction = ((point.x - domain.minX) / domainLength).coerceIn(0.0, 1.0)
        val index = (fraction * bucketCount).toInt().coerceAtMost(bucketCount - 1)
        buckets[index] += point
    }

    val selected = mutableListOf(first, last)
    buckets.forEach { bucket ->
        if (bucket.isEmpty()) return@forEach
        val minPoint = bucket.minBy { requireNotNull(it.y) }
        val maxPoint = bucket.maxBy { requireNotNull(it.y) }
        selected += minPoint
        selected += maxPoint
    }

    val bounded =
        selected
            .distinctBy { it.x }
            .sortedBy { it.x }
            .let { sorted ->
                if (sorted.size <= budget) {
                    sorted
                } else {
                    val interior = sorted.drop(1).dropLast(1).take(budget - 2)
                    listOf(sorted.first()) + interior + sorted.last()
                }
            }

    return insertGapMarkers(normalized, bounded)
}

private fun insertGapMarkers(
    normalized: List<ChartPoint>,
    selected: List<ChartPoint>,
): List<ChartPoint> {
    if (selected.size < 2) return selected
    val result = mutableListOf<ChartPoint>()
    selected.forEachIndexed { index, point ->
        if (index > 0) {
            val previous = selected[index - 1]
            normalized.firstOrNull {
                it.y == null && it.x > previous.x && it.x < point.x
            }?.let(result::add)
        }
        result += point
    }
    return result.sortedBy { it.x }
}

fun prepareLineSeriesForRender(
    series: LineChartSeries,
    domain: ChartDomain,
    maxRenderPoints: Int = DefaultMaxRenderPoints,
): LineChartSeries =
    series.copy(
        points =
            if (series.allowReduction) {
                reduceMinMax(series.points, domain, maxRenderPoints)
            } else {
                normalizeChartPoints(series.points, domain)
            },
    )

fun markerRowsAtX(
    series: List<LineChartSeries>,
    selectedX: Double,
): List<ChartMarkerRow> =
    series.mapNotNull { item ->
        val value =
            item.points
                .lastOrNull { it.x == selectedX }
                ?.y
                ?.takeIf(Double::isFinite)
                ?: return@mapNotNull null
        ChartMarkerRow(
            label = item.label,
            value = value,
            style = item.style,
            valueFormat = item.valueFormat,
        )
    }
