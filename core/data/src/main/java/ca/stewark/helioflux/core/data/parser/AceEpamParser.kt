package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.AceEpamSample
import java.time.LocalDateTime
import java.time.ZoneOffset

object AceEpamParser {
    fun parse(text:String):List<AceEpamSample> = text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { it.trim().split(Regex("""\s+""")) }
        .filter { it.size >= 16 }
        .mapNotNull { p ->
            val hhmm=p[3].padStart(4,'0')
            val ts=try { LocalDateTime.of(p[0].toInt(),p[1].toInt(),p[2].toInt(),hhmm.take(2).toInt(),hhmm.takeLast(2).toInt()).toInstant(ZoneOffset.UTC).toEpochMilli() } catch(_:Exception){ return@mapNotNull null }
            fun value(index:Int)=p[index].toDoubleOrNull()?.takeIf { it > -1.0e5 }
            AceEpamSample(ts,value(7),value(8),value(10),value(11),value(12))
        }.toList()

    fun last72Hours(samples:List<AceEpamSample>, nowMillis:Long):List<AceEpamSample> =
        samples.filter { it.timestampMillis >= nowMillis - 72L*60*60*1000 }.sortedBy { it.timestampMillis }
}
