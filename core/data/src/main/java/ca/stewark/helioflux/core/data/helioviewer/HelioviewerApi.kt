package ca.stewark.helioflux.core.data.helioviewer

import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpTransport
import kotlinx.serialization.json.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class HelioviewerImage(val id:String,val date:String,val name:String?)

class HelioviewerApi(private val transport:HttpTransport) {
    suspend fun getClosestImage(isoDate:String, sourceId:Int=13):HelioviewerImage {
        val date=URLEncoder.encode(isoDate,StandardCharsets.UTF_8.toString()).replace("+","%20")
        val result=transport.get(HelioFluxEndpoints.helioviewer+"/getClosestImage/?date="+date+"&sourceId="+sourceId)
        require(result.status in 200..299) { "HTTP ${result.status}" }
        val o=Json.parseToJsonElement(result.body).jsonObject
        return HelioviewerImage(o["id"]!!.jsonPrimitive.content,o["date"]!!.jsonPrimitive.content,o["name"]?.jsonPrimitive?.contentOrNull)
    }

    fun downloadUrl(imageId:String,width:Int=512):String {
        val id=URLEncoder.encode(imageId,StandardCharsets.UTF_8.toString())
        return HelioFluxEndpoints.helioviewer+"/downloadImage/?id="+id+"&width="+width+"&type=png"
    }
}
