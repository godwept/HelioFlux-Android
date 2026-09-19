package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.XrayFluxSample
import kotlinx.serialization.json.*

object XrayFluxParser {
    fun parse(json: String): List<XrayFluxSample> {
        data class MutableChannels(var s18:Double?=null,var l18:Double?=null,var s19:Double?=null,var l19:Double?=null)
        val byTime = sortedMapOf<Long, MutableChannels>()
        Json.parseToJsonElement(json).jsonArray.forEach { element ->
            val o=element.jsonObject
            val time=timestampMillis(o["time_tag"]?.jsonPrimitive?.content ?: return@forEach) ?: return@forEach
            val satellite=o["satellite"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val energy=o["energy"]?.jsonPrimitive?.content ?: return@forEach
            val raw=o["flux"]?.jsonPrimitive?.doubleOrNull
            val flux=raw?.takeIf { it > 0 }
            val c=byTime.getOrPut(time){MutableChannels()}
            when {
                satellite==18 && energy=="0.05-0.4nm" -> c.s18=flux
                satellite==18 && energy=="0.1-0.8nm" -> c.l18=flux
                satellite==19 && energy=="0.05-0.4nm" -> c.s19=flux
                satellite==19 && energy=="0.1-0.8nm" -> c.l19=flux
            }
        }
        return byTime.map { (t,c) -> XrayFluxSample(t,c.s18,c.l18,c.s19,c.l19) }
    }
}
