package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.ForecastSection

object ForecastDiscussionParser {
    private val definitions = listOf(
        "solar" to "Solar Activity",
        "particle" to "Energetic Particle",
        "wind" to "Solar Wind",
        "geospace" to "Geospace",
    )

    fun parse(text: String): List<ForecastSection> {
        val issue = Regex(""":Issued:\s*(.+)""").find(text)?.groupValues?.get(1)?.trim()
        val cleaned = text.lines()
            .filterNot { it.trimStart().startsWith("#") || it.startsWith(":") }
            .joinToString("\n")

        return definitions.mapIndexedNotNull { index, (key, title) ->
            val start = cleaned.indexOf(title)
            if (start < 0) return@mapIndexedNotNull null
            val nextTitle = definitions.getOrNull(index + 1)?.second
            val end = nextTitle?.let { cleaned.indexOf(it, start + title.length).takeIf { found -> found >= 0 } } ?: cleaned.length
            val body = cleaned.substring(start + title.length, end)
            val summary = Regex("""\.24 hr Summary\.\.\.\s*([\s\S]*?)(?=\.Forecast\.\.\.|$)""")
                .find(body)?.groupValues?.get(1)?.let(::normalize).orEmpty()
            val forecast = Regex("""\.Forecast\.\.\.\s*([\s\S]*?)$""")
                .find(body)?.groupValues?.get(1)?.let(::normalize).orEmpty()
            ForecastSection(key, title, summary, forecast, issue)
        }
    }

    private fun normalize(raw: String): String = raw
        .replace("\r", "")
        .replace(Regex("""([^\n])\n([^\n])"""), "$1 $2")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .replace(Regex(""" +"""), " ")
        .trim()
}
