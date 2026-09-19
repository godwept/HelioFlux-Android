package ca.stewark.helioflux.core.data.parser

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun timestampMillis(value: String): Long? = try {
    if (value.endsWith("Z")) Instant.parse(value).toEpochMilli()
    else LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
} catch (_: Exception) {
    null
}
