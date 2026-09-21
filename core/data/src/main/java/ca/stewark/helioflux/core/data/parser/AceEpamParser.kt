package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.AceEpamSample
import kotlinx.serialization.json.*

object AceEpamParser {
    fun parse(json: String): List<AceEpamSample> =
        Json.parseToJsonElement(json)
            .jsonArray
            .mapNotNull { element ->
                val values = element.jsonObject
                val timestamp =
                    timestampMillis(
                        values["time_tag"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    ) ?: return@mapNotNull null

                fun value(vararg keys: String): Double? =
                    keys.firstNotNullOfOrNull { key ->
                        values[key]
                            ?.jsonPrimitive
                            ?.doubleOrNull
                            ?.takeIf { it.isFinite() && it > -9.0e4 }
                    }

                AceEpamSample(
                    timestampMillis = timestamp,
                    electronLow = value("de1"),
                    electronHigh = value("de4"),
                    protonLow = value("p1"),
                    protonMid = value("p3"),
                    protonHigh = value("p5"),
                    protonP7 = value("p7"),
                    protonP8 = value("p8"),
                )
            }
            .sortedBy(AceEpamSample::timestampMillis)

    fun last72Hours(samples: List<AceEpamSample>, nowMillis: Long): List<AceEpamSample> =
        samples
            .filter { it.timestampMillis >= nowMillis - 72L * 60L * 60L * 1_000L }
            .sortedBy(AceEpamSample::timestampMillis)
}
