package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.ui.graphics.Color
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.CmeEvent
import ca.stewark.helioflux.core.model.FlareEvent
import ca.stewark.helioflux.core.model.FlareProbabilities
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.FreshGreen
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.WarningAmber
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SolarActivityUtcFormatter =
    DateTimeFormatter
        .ofPattern("MMM dd, HH:mm 'UTC'", Locale.US)
        .withZone(ZoneOffset.UTC)

internal fun formatSolarActivityUtc(timestampMillis: Long): String =
    SolarActivityUtcFormatter.format(Instant.ofEpochMilli(timestampMillis))

data class FlareProbabilityMetric(
    val label: String,
    val value: String,
    val accent: Color,
)

data class FlareProbabilityPresentation(
    val metrics: List<FlareProbabilityMetric>,
    val message: String?,
)

data class FlareEventPresentation(
    val flareClass: String,
    val time: String,
    val metadata: String,
    val accent: Color,
)

data class CmeEventPresentation(
    val speed: String,
    val time: String,
    val metadata: String,
    val emphasis: CmeSpeedEmphasis,
)

internal fun flareProbabilityPresentation(
    state: RepositoryState<FlareProbabilities>,
): FlareProbabilityPresentation {
    val data =
        when (state) {
            is RepositoryState.Available -> state.data
            is RepositoryState.Failure -> state.retainedData
            else -> null
        }
    val metrics =
        data?.let {
            listOf(
                FlareProbabilityMetric("C", "${it.c}%", WarningAmber),
                FlareProbabilityMetric("M", "${it.m}%", SolarOrange),
                FlareProbabilityMetric("X", "${it.x}%", AlertRed),
            )
        }.orEmpty()
    val message =
        when {
            state is RepositoryState.Loading -> "Loading flare probabilities"
            state is RepositoryState.Failure && state.retainedData != null ->
                "Showing cached flare probabilities"
            state is RepositoryState.Empty ||
                (state is RepositoryState.Failure && state.retainedData == null) ->
                "Flare probabilities unavailable"
            else -> null
        }
    return FlareProbabilityPresentation(metrics, message)
}

fun flareClassAccent(value: String): Color =
    when (flareClassGroup(value)) {
        'A' -> SpaceMuted
        'B' -> FreshGreen
        'C' -> DataCyan
        'M' -> SolarOrange
        'X' -> AlertRed
        else -> SpaceMuted
    }

internal fun flareEventPresentation(flare: FlareEvent): FlareEventPresentation {
    val metadata =
        buildList {
            flare.observatory.takeIf { it.isNotBlank() }?.let(::add)
            flare.region?.takeIf { it.isNotBlank() }?.let { add("AR $it") }
            flare.location?.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(" · ")
    return FlareEventPresentation(
        flareClass = flare.flareClass,
        time = formatSolarActivityUtc(flare.timestampMillis),
        metadata = metadata,
        accent = flareClassAccent(flare.flareClass),
    )
}

internal fun cmeEventPresentation(cme: CmeEvent): CmeEventPresentation {
    val metadata =
        buildList {
            cme.direction?.takeIf { it.isNotBlank() }?.let(::add)
            cme.halfAngle?.let { add("${(it * 2).toInt()}° wide") }
        }.joinToString(" · ")
    return CmeEventPresentation(
        speed = cme.speed?.let { "${it.toInt()} km/s" } ?: "Speed unavailable",
        time = formatSolarActivityUtc(cme.timestampMillis),
        metadata = metadata,
        emphasis = cmeSpeedEmphasis(cme.speed),
    )
}
