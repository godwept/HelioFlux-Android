package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.HemisphericPowerSample

object HemisphericPowerParser {
    private val timestampPattern = Regex("""\d{4}-\d{2}-\d{2}_\d{2}:\d{2}""")

    fun parse(text: String): List<HemisphericPowerSample> = text.lineSequence().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        val parts = trimmed.split(Regex("""\s+"""))
        if (parts.size < 4 || !timestampPattern.matches(parts[0])) return@mapNotNull null
        val ts = timestampMillis(parts[0].replace('_', 'T') + ":00Z") ?: return@mapNotNull null
        val north = parts[2].toDoubleOrNull() ?: return@mapNotNull null
        val south = parts[3].toDoubleOrNull() ?: return@mapNotNull null
        HemisphericPowerSample(ts, north, south)
    }.toList()
}
