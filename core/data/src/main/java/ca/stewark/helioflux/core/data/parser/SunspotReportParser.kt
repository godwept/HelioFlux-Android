package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.ActiveRegion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

object SunspotReportParser {
    private val locationPattern = Regex("""^([NS])(\d{1,2})([EW])(\d{1,3})\*?$""")

    private data class Candidate(
        val regionNumber: Int,
        val observationTimestampMillis: Long,
        val latitudeDegrees: Double,
        val centralMeridianDegrees: Double,
    )

    fun parse(
        json: String,
        targetTimestampMillis: Long,
    ): List<ActiveRegion> {
        val selected = linkedMapOf<Int, Candidate>()
        Json.parseToJsonElement(json).jsonArray.forEach { element ->
            val objectValue = element.jsonObject
            val regionNumber =
                objectValue["Region"]?.jsonPrimitive?.contentOrNull?.trim()?.toIntOrNull()
                    ?: return@forEach
            if (regionNumber <= 0) return@forEach

            val timestamp =
                objectValue["time_tag"]?.jsonPrimitive?.contentOrNull?.let(::timestampMillis)
                    ?: return@forEach
            val location =
                objectValue["Location"]?.jsonPrimitive?.contentOrNull
                    ?: objectValue["Report_Location"]?.jsonPrimitive?.contentOrNull
                    ?: return@forEach
            val parsedLocation = parseLocation(location) ?: return@forEach
            if (abs(parsedLocation.second) > 90.0) return@forEach

            val candidate =
                Candidate(
                    regionNumber = regionNumber,
                    observationTimestampMillis = timestamp,
                    latitudeDegrees = parsedLocation.first,
                    centralMeridianDegrees = parsedLocation.second,
                )
            val current = selected[regionNumber]
            if (current == null || isCloser(candidate, current, targetTimestampMillis)) {
                selected[regionNumber] = candidate
            }
        }

        val b0 = solarB0Degrees(targetTimestampMillis)
        return selected.values
            .sortedBy(Candidate::regionNumber)
            .map { candidate ->
                val (x, y) =
                    projectToHmi(
                        latitudeDegrees = candidate.latitudeDegrees,
                        centralMeridianDegrees = candidate.centralMeridianDegrees,
                        b0Degrees = b0,
                    )
                val label = candidate.regionNumber.toString()
                ActiveRegion(
                    id = label,
                    number = label,
                    helioprojectiveX = x,
                    helioprojectiveY = y,
                )
            }
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

    private fun isCloser(
        candidate: Candidate,
        current: Candidate,
        targetTimestampMillis: Long,
    ): Boolean {
        val candidateDistance = abs(candidate.observationTimestampMillis - targetTimestampMillis)
        val currentDistance = abs(current.observationTimestampMillis - targetTimestampMillis)
        return when {
            candidateDistance < currentDistance -> true
            candidateDistance > currentDistance -> false
            else -> candidate.observationTimestampMillis > current.observationTimestampMillis
        }
    }

    private fun normalizeDegrees(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0

    private const val SOLAR_RADIUS_UNITS = 1000.0
    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val JULIAN_UNIX_EPOCH = 2_440_587.5
    private const val J2000 = 2_451_545.0
    private const val DAYS_PER_JULIAN_CENTURY = 36_525.0
}
