package ca.stewark.helioflux.core.data.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class HmibcImageRecord(
    val timestampMillis: Long,
    val url: String,
)

internal object HmibcMetadataParser {
    fun parse(json: String): HmibcImageRecord? {
        val samples =
            Json.parseToJsonElement(json)
                .jsonObject["iswa_sdo_aia_hmic_files"]
                ?.jsonObject
                ?.get("samples")
                ?.jsonArray
                ?: return null
        val sample = samples.singleOrNull()?.jsonObject ?: return null
        val timestamp = sample["timestamp"]?.jsonPrimitive?.contentOrNull ?: return null
        val url = sample["url"]?.jsonPrimitive?.contentOrNull ?: return null
        if (!url.startsWith("https://")) return null

        val timestampMillis = timestampMillis(timestamp.replaceFirst(" ", "T")) ?: return null
        return HmibcImageRecord(timestampMillis, url)
    }
}
