package ca.stewark.helioflux.core.database

object RetentionPolicy {
    const val HOUR_MILLIS = 60L * 60 * 1000
    const val NORMAL_SERIES_MILLIS = 54 * HOUR_MILLIS
    const val SOLAR_ACTIVITY_SERIES_MILLIS = 78 * HOUR_MILLIS
    const val EVENT_MILLIS = 78 * HOUR_MILLIS
    const val OVATION_SNAPSHOT_COUNT = 3

    fun normalSeriesCutoff(nowMillis: Long) = nowMillis - NORMAL_SERIES_MILLIS
    fun solarActivityCutoff(nowMillis: Long) = nowMillis - SOLAR_ACTIVITY_SERIES_MILLIS
    fun eventCutoff(nowMillis: Long) = nowMillis - EVENT_MILLIS
}
