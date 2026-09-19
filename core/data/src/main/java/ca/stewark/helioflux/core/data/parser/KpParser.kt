package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.KpSample
import kotlinx.serialization.json.*

object KpParser {
    fun parse(json: String): List<KpSample> = Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
        val o = element.jsonObject
        val ts = timestampMillis(o["time_tag"]?.jsonPrimitive?.content ?: return@mapNotNull null) ?: return@mapNotNull null
        KpSample(ts, o["Kp"]?.jsonPrimitive?.content?.toDoubleOrNull())
    }
}
