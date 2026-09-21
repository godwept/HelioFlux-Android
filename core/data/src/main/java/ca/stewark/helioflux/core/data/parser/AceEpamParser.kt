package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.AceEpamSample

object AceEpamParser {
    fun parse(csv: String): List<AceEpamSample> {
        val lines =
            csv.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
        if (lines.isEmpty()) return emptyList()

        val header = splitLine(lines.first()).map(::normalizeHeader)
        val timeIndex = header.indexOf("time")
        if (timeIndex < 0) return emptyList()

        fun index(name: String): Int = header.indexOf(name)

        val p1Index = index("protonflux_47_68")
        val p3Index = index("protonflux_115_195")
        val p5Index = index("protonflux_310_580")
        val fp6Index = index("protonflux_795_1193")
        val p7Index = index("protonflux_1060_1900")

        return lines
            .drop(1)
            .mapNotNull { line ->
                val values = splitLine(line)
                val timestamp =
                    values.getOrNull(timeIndex)
                        ?.let(::timestampMillis)
                        ?: return@mapNotNull null

                fun value(column: Int): Double? =
                    values
                        .getOrNull(column)
                        ?.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it > -9.0e4 }

                AceEpamSample(
                    timestampMillis = timestamp,
                    electronLow = null,
                    electronHigh = null,
                    protonLow = value(p1Index),
                    protonMid = value(p3Index),
                    protonHigh = value(p5Index),
                    protonFp6 = value(fp6Index),
                    protonP7 = value(p7Index),
                )
            }
            .sortedBy(AceEpamSample::timestampMillis)
    }

    fun last72Hours(samples: List<AceEpamSample>, nowMillis: Long): List<AceEpamSample> =
        samples
            .filter { it.timestampMillis >= nowMillis - 72L * 60L * 60L * 1_000L }
            .sortedBy(AceEpamSample::timestampMillis)

    private fun splitLine(line: String): List<String> =
        line.split(',').map { value -> value.trim().trim('"') }

    private fun normalizeHeader(value: String): String =
        value.substringBefore(" (").trim().lowercase()
}
