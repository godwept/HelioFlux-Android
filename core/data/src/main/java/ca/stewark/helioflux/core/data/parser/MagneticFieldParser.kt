package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.SolarWindMag
import kotlinx.serialization.json.*

object MagneticFieldParser {
    fun parse(json: String): List<SolarWindMag> {
        val root = Json.parseToJsonElement(json).jsonArray
        if (root.isEmpty()) return emptyList()
        return if (root.first() is JsonObject) {
            root.mapNotNull { element ->
                val o = element.jsonObject
                val ts = timestampMillis(o["time_tag"]?.jsonPrimitive?.content ?: return@mapNotNull null) ?: return@mapNotNull null
                SolarWindMag(ts, o["bx_gsm"]?.jsonPrimitive?.content?.toDoubleOrNull(), o["by_gsm"]?.jsonPrimitive?.content?.toDoubleOrNull(), o["bz_gsm"]?.jsonPrimitive?.content?.toDoubleOrNull(), o["bt"]?.jsonPrimitive?.content?.toDoubleOrNull())
            }
        } else {
            root.drop(1).mapNotNull { element ->
                val r = element.jsonArray
                val ts = timestampMillis(r.getOrNull(0)?.jsonPrimitive?.content ?: return@mapNotNull null) ?: return@mapNotNull null
                SolarWindMag(ts, r.getOrNull(1)?.jsonPrimitive?.content?.toDoubleOrNull(), r.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull(), r.getOrNull(3)?.jsonPrimitive?.content?.toDoubleOrNull(), r.getOrNull(6)?.jsonPrimitive?.content?.toDoubleOrNull())
            }
        }
    }
}
