package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.ActiveRegion
import kotlinx.serialization.json.*
import kotlin.math.abs

object ActiveRegionParser {
    fun parse(json:String):List<ActiveRegion> {
        val root=Json.parseToJsonElement(json).jsonObject
        val deduped=linkedMapOf<String,ActiveRegion>()
        root["result"]?.jsonArray.orEmpty().forEach { element ->
            val o=element.jsonObject
            val id=o["ar_noaanum"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val x=o["hpc_x"]?.jsonPrimitive?.doubleOrNull
            val y=o["hpc_y"]?.jsonPrimitive?.doubleOrNull
            if(id.isEmpty() || x==null || y==null || abs(x)>=1000 || abs(y)>=1000) return@forEach
            val number=if(id.startsWith("1") && id.length>=5) id.drop(1) else id
            deduped[id]=ActiveRegion(id,number,x,y)
        }
        return deduped.values.toList()
    }
}
