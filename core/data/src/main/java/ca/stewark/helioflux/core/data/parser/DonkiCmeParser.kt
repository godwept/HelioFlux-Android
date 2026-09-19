package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.CmeEvent
import kotlinx.serialization.json.*
import kotlin.math.abs

object DonkiCmeParser {
    fun parse(json:String):List<CmeEvent> = Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
        val o=element.jsonObject
        val raw=o["associatedCMEstartTime"]?.jsonPrimitive?.contentOrNull ?: o["time21_5"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val time=timestampMillis(if(raw.endsWith("Z")) raw else raw+"Z") ?: return@mapNotNull null
        val lat=o["latitude"]?.jsonPrimitive?.doubleOrNull
        val lon=o["longitude"]?.jsonPrimitive?.doubleOrNull
        val direction=if(lat!=null && lon!=null) "${abs(lat)}°${if(lat>=0) "N" else "S"} ${abs(lon)}°${if(lon>=0) "E" else "W"}" else null
        CmeEvent(
            id=o["associatedCMEID"]?.jsonPrimitive?.contentOrNull ?: raw,
            timestampMillis=time,
            speed=o["speed"]?.jsonPrimitive?.doubleOrNull,
            halfAngle=o["halfAngle"]?.jsonPrimitive?.doubleOrNull,
            direction=direction,
            type=o["type"]?.jsonPrimitive?.contentOrNull,
            link=o["associatedCMELink"]?.jsonPrimitive?.contentOrNull,
        )
    }.sortedByDescending { it.timestampMillis }
}
