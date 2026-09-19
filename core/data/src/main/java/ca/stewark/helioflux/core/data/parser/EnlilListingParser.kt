package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.EnlilFrame
import java.time.LocalDateTime
import java.time.ZoneOffset

object EnlilListingParser {
    private val href=Regex("""href="([^"]+?\.(?:jpg|png))"""",RegexOption.IGNORE_CASE)
    private val run=Regex("""enlil_com2_(\d+)_""")
    private val stamp=Regex("""(\d{8}[T_]?\d{6})""")

    fun parse(listing:String, baseUrl:String):List<EnlilFrame> {
        data class Raw(val run:String,val time:Long,val file:String)
        val raw=href.findAll(listing).mapNotNull { match ->
            val file=match.groupValues[1]
            val runKey=run.find(file)?.groupValues?.get(1) ?: return@mapNotNull null
            val token=stamp.find(file)?.groupValues?.get(1) ?: return@mapNotNull null
            val time=parseCompact(token) ?: return@mapNotNull null
            Raw(runKey,time,file)
        }.toList()
        if(raw.isEmpty()) return emptyList()
        val latestGroup=raw.groupBy { it.run }.values.maxByOrNull { group -> group.maxOf { it.time } } ?: return emptyList()
        val sorted=latestGroup.sortedBy { it.time }
        val step=maxOf(1,kotlin.math.ceil(sorted.size/48.0).toInt())
        val runTime=latestGroup.maxOf { it.time }
        return sorted.filterIndexed { index,_ -> index%step==0 }.map { EnlilFrame(runTime,it.time,baseUrl.trimEnd('/')+"/"+it.file) }
    }

    private fun parseCompact(value:String):Long? = try {
        val s=value.replace("_","T")
        LocalDateTime.of(s.substring(0,4).toInt(),s.substring(4,6).toInt(),s.substring(6,8).toInt(),s.substring(9,11).toInt(),s.substring(11,13).toInt(),s.substring(13,15).toInt()).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch(_:Exception){ null }
}
