package ca.stewark.helioflux.feature.alerts

data class NotificationText(val title: String, val body: String)

class SpaceWeatherNotificationFormatter {
    fun geomagnetic(band: Int, escalation: Boolean): NotificationText =
        if (escalation) NotificationText(
            title = "Geomagnetic storm intensifying",
            body = "Kp has escalated to $band."
        ) else NotificationText(
            title = "Geomagnetic storm watch",
            body = "Kp has reached $band."
        )

    fun flare(flareClass: String): NotificationText {
        val normalized = flareClass.trim().uppercase()
        val severity = normalized.firstOrNull()
        val title = if (severity == 'X') "Major solar flare" else "Solar flare"
        return NotificationText(title, "$normalized-class flare detected.")
    }
}
