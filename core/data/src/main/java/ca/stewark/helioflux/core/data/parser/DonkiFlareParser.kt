package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.FlareEvent
import kotlinx.serialization.json.*

object DonkiFlareParser {
    fun parse(json:String):List<FlareEvent> = Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
        val o=element.jsonObject
        val raw=o["peakTime"]?.jsonPrimitive?.contentOrNull ?: o["beginTime"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val time=timestampMillis(if(raw.endsWith("Z")) raw else raw+"Z") ?: return@mapNotNull null
        val id=o["flrID"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val region=o["activeRegionNum"]?.jsonPrimitive?.intOrNull?.let { it % 10000 }?.takeIf { it != 0 }?.toString()
        val instrument=o["instruments"]?.jsonArray?.firstOrNull()?.jsonObject?.get("displayName")?.jsonPrimitive?.contentOrNull
        val observatory=Regex("""GOES-\w+""").find(instrument.orEmpty())?.value ?: "GOES"
        FlareEvent(id,o["classType"]?.jsonPrimitive?.contentOrNull ?: "?",time,observatory,region,o["sourceLocation"]?.jsonPrimitive?.contentOrNull)
    }.sortedByDescending { it.timestampMillis }
}
