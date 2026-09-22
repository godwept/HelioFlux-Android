package ca.stewark.helioflux.core.data.parser

import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun timestampMillis(value: String): Long? = try {
    if (value.endsWith("Z")) OffsetDateTime.parse(value).toInstant().toEpochMilli()
    else LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
} catch (_: Exception) {
    null
}
