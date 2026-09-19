package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.FlareProbabilities

object FlareProbabilityParser {
    fun parse(text: String): FlareProbabilities {
        var inClassC = false
        var maxC = 0
        var maxM = 0
        var maxX = 0
        text.lineSequence().forEach { line ->
            if (line.startsWith("#") && line.contains("Class C")) {
                inClassC = true
                return@forEach
            }
            if (!inClassC || line.isBlank() || line.startsWith(":") || line.startsWith("#")) return@forEach
            val parts = line.trim().split(Regex("""\s+"""))
            if (parts.size < 5) return@forEach
            maxC = maxOf(maxC, parts[1].toIntOrNull() ?: 0)
            maxM = maxOf(maxM, parts[2].toIntOrNull() ?: 0)
            maxX = maxOf(maxX, parts[3].toIntOrNull() ?: 0)
        }
        return FlareProbabilities(maxC, maxM, maxX)
    }
}
