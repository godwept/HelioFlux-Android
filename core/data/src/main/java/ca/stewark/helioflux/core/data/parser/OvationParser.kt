package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.AuroraPoint
import ca.stewark.helioflux.core.model.AuroraSnapshot
import kotlinx.serialization.json.*

object OvationParser {
    fun parse(json: String): AuroraSnapshot? {
        val o = Json.parseToJsonElement(json).jsonObject
        val observation = timestampMillis(o["Observation Time"]?.jsonPrimitive?.content ?: return null) ?: return null
        val forecast = timestampMillis(o["Forecast Time"]?.jsonPrimitive?.content ?: return null) ?: return null
        val points = o["coordinates"]?.jsonArray.orEmpty().mapNotNull { element ->
            val r = element.jsonArray
            val longitude = r.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val latitude = r.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val intensity = r.getOrNull(2)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            if (intensity < 5 || (intensity < 9 && longitude % 2.0 != 0.0)) null
            else AuroraPoint(latitude, longitude, intensity)
        }
        return AuroraSnapshot(observation, forecast, points)
    }
}
