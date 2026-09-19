package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.SolarWindPlasma
import kotlinx.serialization.json.*

object PlasmaParser {
    fun parse(json: String): List<SolarWindPlasma> {
        val root = Json.parseToJsonElement(json).jsonArray
        if (root.isEmpty()) return emptyList()
        val rows = if (root.first() is JsonObject) {
            root.mapNotNull { element ->
                val o = element.jsonObject
                val ts = timestampMillis(o["time_tag"]?.jsonPrimitive?.content ?: return@mapNotNull null) ?: return@mapNotNull null
                SolarWindPlasma(ts, o["proton_density"]?.jsonPrimitive?.content?.toDoubleOrNull(), o["proton_speed"]?.jsonPrimitive?.content?.toDoubleOrNull(), o["proton_temperature"]?.jsonPrimitive?.content?.toDoubleOrNull())
            }
        } else {
            root.drop(1).mapNotNull { element ->
                val r = element.jsonArray
                val ts = timestampMillis(r.getOrNull(0)?.jsonPrimitive?.content ?: return@mapNotNull null) ?: return@mapNotNull null
                SolarWindPlasma(ts, r.getOrNull(1)?.jsonPrimitive?.content?.toDoubleOrNull(), r.getOrNull(2)?.jsonPrimitive?.content?.toDoubleOrNull(), r.getOrNull(3)?.jsonPrimitive?.content?.toDoubleOrNull())
            }
        }
        return rows.filterNot { (it.speed ?: 0.0) == 0.0 && (it.density ?: 0.0) == 0.0 }
    }
}
