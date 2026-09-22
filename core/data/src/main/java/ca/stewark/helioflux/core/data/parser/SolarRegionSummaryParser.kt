package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.ActiveRegion
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

object SolarRegionSummaryParser {
    private enum class Section {
        NONE,
        SUNSPOT,
        PLAGE,
    }

    private data class SummaryRegion(
        val number: String,
        val label: String,
        val latitudeDegrees: Double,
        val centralMeridianDegrees: Double,
    )

    private val issuedPattern =
        Regex("""^:Issued:\s+(\d{4})\s+([A-Za-z]{3})\s+(\d{1,2})\s+(\d{4})\s+UTC\s*$""")
    private val validPattern = Regex("""Locations Valid at\s+(\d{1,2})/(\d{4})Z""")
    private val rowPattern = Regex("""^\s*(\d{4})\s+([NS]\d{2}[EW]\d{2,3})\b.*$""")
    private val locationPattern = Regex("""^([NS])(\d{2})([EW])(\d{2,3})$""")
    private val issuedFormatter =
        DateTimeFormatter.ofPattern("yyyy MMM d HHmm", Locale.US)

    fun parse(
        text: String,
        targetTimestampMillis: Long,
    ): List<ActiveRegion> {
        val lines = text.lines()
        val issuedInstant = parseIssuedInstant(lines) ?: return emptyList()
        val validInstant = parseValidInstant(lines, issuedInstant) ?: return emptyList()
        if (abs(targetTimestampMillis - validInstant.toEpochMilli()) > MAX_SUMMARY_AGE_MILLIS) {
            return emptyList()
        }

        var section = Section.NONE
        val regions = mutableListOf<SummaryRegion>()
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.startsWith("I.  Regions with Sunspots.") ||
                    line.startsWith("I. Regions with Sunspots.") -> {
                    section = Section.SUNSPOT
                    return@forEach
                }
                line.startsWith("IA.") -> {
                    section = Section.PLAGE
                    return@forEach
                }
                line.startsWith("II.") -> {
                    section = Section.NONE
                    return@forEach
                }
            }
            if (section == Section.NONE) return@forEach

            val match = rowPattern.matchEntire(line) ?: return@forEach
            val number = match.groupValues[1]
            val location = parseLocation(match.groupValues[2]) ?: return@forEach
            regions +=
                SummaryRegion(
                    number = number,
                    label = if (section == Section.PLAGE) "($number)" else number,
                    latitudeDegrees = location.first,
                    centralMeridianDegrees = location.second,
                )
        }

        val elapsedDays =
            (targetTimestampMillis - validInstant.toEpochMilli()) / MILLIS_PER_DAY
        val b0 = solarB0Degrees(targetTimestampMillis)

        return regions.mapNotNull { region ->
            val rotatedCentralMeridian =
                region.centralMeridianDegrees +
                    synodicRotationDegreesPerDay(region.latitudeDegrees) * elapsedDays
            if (abs(rotatedCentralMeridian) > 90.0) return@mapNotNull null

            val projected =
                projectToHmi(
                    latitudeDegrees = region.latitudeDegrees,
                    centralMeridianDegrees = rotatedCentralMeridian,
                    b0Degrees = b0,
                )
            ActiveRegion(
                id = region.number,
                number = region.label,
                helioprojectiveX = projected.first,
                helioprojectiveY = projected.second,
            )
        }.sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
    }

    internal fun solarB0Degrees(timestampMillis: Long): Double {
        val julianDay = timestampMillis / MILLIS_PER_DAY + JULIAN_UNIX_EPOCH
        val centuries = (julianDay - J2000) / DAYS_PER_JULIAN_CENTURY

        val meanLongitude =
            normalizeDegrees(
                280.46646 +
                    36000.76983 * centuries +
                    0.0003032 * centuries * centuries,
            )
        val meanAnomaly =
            Math.toRadians(
                normalizeDegrees(
                    357.52911 +
                        35999.05029 * centuries -
                        0.0001537 * centuries * centuries,
                ),
            )
        val equationOfCenter =
            (1.914602 - 0.004817 * centuries - 0.000014 * centuries * centuries) *
                sin(meanAnomaly) +
                (0.019993 - 0.000101 * centuries) * sin(2.0 * meanAnomaly) +
                0.000289 * sin(3.0 * meanAnomaly)
        val trueLongitude = normalizeDegrees(meanLongitude + equationOfCenter)
        val omega = Math.toRadians(125.04 - 1934.136 * centuries)
        val apparentLongitude =
            normalizeDegrees(trueLongitude - 0.00569 - 0.00478 * sin(omega))
        val ascendingNode =
            73.6667 +
                1.3958333 * (julianDay - 2396758.0) / DAYS_PER_JULIAN_CENTURY
        val inclination = Math.toRadians(7.25)

        return Math.toDegrees(
            asin(
                sin(Math.toRadians(apparentLongitude - ascendingNode)) *
                    sin(inclination),
            ),
        )
    }

    private fun parseIssuedInstant(lines: List<String>): Instant? {
        val match =
            lines.firstNotNullOfOrNull { issuedPattern.matchEntire(it.trim()) }
                ?: return null
        return runCatching {
            val value =
                match.groupValues[1] + " " +
                    match.groupValues[2] + " " +
                    match.groupValues[3] + " " +
                    match.groupValues[4]
            LocalDateTime.parse(value, issuedFormatter).toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    private fun parseValidInstant(
        lines: List<String>,
        issuedInstant: Instant,
    ): Instant? {
        val match =
            lines.firstNotNullOfOrNull { validPattern.find(it) }
                ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val hhmm = match.groupValues[2].toIntOrNull() ?: return null
        val issuedDate = issuedInstant.atZone(ZoneOffset.UTC).toLocalDate()

        val dates =
            (-2L..2L)
                .map(issuedDate::plusDays)
                .filter { it.dayOfMonth == day }
        return dates.mapNotNull { date ->
            validInstant(date, hhmm)
        }.minByOrNull { instant ->
            abs(Duration.between(instant, issuedInstant).toMillis())
        }
    }

    private fun validInstant(
        date: LocalDate,
        hhmm: Int,
    ): Instant? {
        if (hhmm == 2400) {
            return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        }
        val hour = hhmm / 100
        val minute = hhmm % 100
        if (hour !in 0..23 || minute !in 0..59) return null
        return date.atTime(hour, minute).toInstant(ZoneOffset.UTC)
    }

    private fun parseLocation(value: String): Pair<Double, Double>? {
        val match = locationPattern.matchEntire(value.trim().uppercase()) ?: return null
        val latitudeMagnitude = match.groupValues[2].toDoubleOrNull() ?: return null
        val longitudeMagnitude = match.groupValues[4].toDoubleOrNull() ?: return null
        if (latitudeMagnitude > 90.0 || longitudeMagnitude > 180.0) return null

        val latitude =
            if (match.groupValues[1] == "S") -latitudeMagnitude else latitudeMagnitude
        val centralMeridian =
            if (match.groupValues[3] == "E") -longitudeMagnitude else longitudeMagnitude
        return latitude to centralMeridian
    }

    private fun synodicRotationDegreesPerDay(latitudeDegrees: Double): Double {
        val sinLatitude = sin(Math.toRadians(latitudeDegrees))
        val sinSquared = sinLatitude * sinLatitude
        return SIDEREAL_A -
            SIDEREAL_B * sinSquared -
            SIDEREAL_C * sinSquared * sinSquared -
            EARTH_ORBITAL_DEGREES_PER_DAY
    }

    private fun projectToHmi(
        latitudeDegrees: Double,
        centralMeridianDegrees: Double,
        b0Degrees: Double,
    ): Pair<Double, Double> {
        val latitude = Math.toRadians(latitudeDegrees)
        val centralMeridian = Math.toRadians(centralMeridianDegrees)
        val b0 = Math.toRadians(b0Degrees)

        val x = SOLAR_RADIUS_UNITS * cos(latitude) * sin(centralMeridian)
        val y =
            SOLAR_RADIUS_UNITS *
                (
                    sin(latitude) * cos(b0) -
                        cos(latitude) * cos(centralMeridian) * sin(b0)
                )
        return x to y
    }

    private fun normalizeDegrees(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0

    private const val SOLAR_RADIUS_UNITS = 1000.0
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val MAX_SUMMARY_AGE_MILLIS = 36L * 60L * 60L * 1000L
    private const val JULIAN_UNIX_EPOCH = 2_440_587.5
    private const val J2000 = 2_451_545.0
    private const val DAYS_PER_JULIAN_CENTURY = 36_525.0
    private const val SIDEREAL_A = 14.713
    private const val SIDEREAL_B = 2.396
    private const val SIDEREAL_C = 1.787
    private const val EARTH_ORBITAL_DEGREES_PER_DAY = 0.9856
}
