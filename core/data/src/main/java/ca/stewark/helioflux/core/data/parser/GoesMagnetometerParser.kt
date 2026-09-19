package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.GoesMagSample
import kotlinx.serialization.json.*

data class GoesMagnetometerResult(
    val data: List<GoesMagSample>,
    val primaryLabel: String,
    val secondaryLabel: String,
)

object GoesMagnetometerParser {
    fun parse(sourcesJson: String, primaryJson: String, secondaryJson: String): GoesMagnetometerResult {
        val sourcesRoot = Json.parseToJsonElement(sourcesJson)
        val sources = if (sourcesRoot is JsonArray) sourcesRoot.first().jsonObject else sourcesRoot.jsonObject
        val mags = sources["magnetometers"]!!.jsonObject
        val secondary = Json.parseToJsonElement(secondaryJson).jsonArray.associateBy { it.jsonObject["time_tag"]?.jsonPrimitive?.content }

        fun hp(o: JsonObject?): Double? {
            if (o == null || o["arcjet_flag"]?.jsonPrimitive?.booleanOrNull == true) return null
            return o["Hp"]?.jsonPrimitive?.doubleOrNull
        }

        val data = Json.parseToJsonElement(primaryJson).jsonArray.mapNotNull { element ->
            val o = element.jsonObject
            val tag = o["time_tag"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val ts = timestampMillis(tag) ?: return@mapNotNull null
            GoesMagSample(ts, hp(o), hp(secondary[tag]?.jsonObject))
        }
        return GoesMagnetometerResult(
            data,
            "GOES-" + mags["primary"]!!.jsonPrimitive.content,
            "GOES-" + mags["secondary"]!!.jsonPrimitive.content,
        )
    }
}
