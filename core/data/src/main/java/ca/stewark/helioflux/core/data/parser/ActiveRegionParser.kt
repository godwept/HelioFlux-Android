package ca.stewark.helioflux.core.data.parser

import ca.stewark.helioflux.core.model.ActiveRegion
import kotlinx.serialization.json.*
import kotlin.math.abs

object ActiveRegionParser {
    private data class Candidate(
        val region: ActiveRegion,
        val timestampMillis: Long?,
    )

    fun parse(
        json: String,
        targetTimestampMillis: Long? = null,
    ): List<ActiveRegion> {
        val root = Json.parseToJsonElement(json).jsonObject
        val deduped = linkedMapOf<String, Candidate>()
        root["result"]?.jsonArray.orEmpty().forEach { element ->
            val o = element.jsonObject
            val id = o["ar_noaanum"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val x = o["hpc_x"]?.jsonPrimitive?.doubleOrNull
            val y = o["hpc_y"]?.jsonPrimitive?.doubleOrNull
            if (id.isEmpty() || x == null || y == null || abs(x) >= 1000 || abs(y) >= 1000) {
                return@forEach
            }
            val number = if (id.startsWith("1") && id.length >= 5) id.drop(1) else id
            val observationTimestamp =
                o["event_starttime"]?.jsonPrimitive?.contentOrNull?.let(::timestampMillis)
            val candidate =
                Candidate(
                    ActiveRegion(id, number, x, y),
                    observationTimestamp,
                )
            val existing = deduped[id]
            if (existing == null || candidateIsCloser(existing, candidate, targetTimestampMillis)) {
                deduped[id] = candidate
            }
        }
        return deduped.values.map(Candidate::region)
    }

    private fun candidateIsCloser(
        existing: Candidate,
        candidate: Candidate,
        targetTimestampMillis: Long?,
    ): Boolean {
        if (targetTimestampMillis == null) return true

        val existingTimestamp = existing.timestampMillis
        val candidateTimestamp = candidate.timestampMillis
        if (candidateTimestamp == null) return existingTimestamp == null
        if (existingTimestamp == null) return true

        val existingDistance = abs(existingTimestamp - targetTimestampMillis)
        val candidateDistance = abs(candidateTimestamp - targetTimestampMillis)
        return when {
            candidateDistance < existingDistance -> true
            candidateDistance > existingDistance -> false
            else -> candidateTimestamp > existingTimestamp
        }
    }
}
