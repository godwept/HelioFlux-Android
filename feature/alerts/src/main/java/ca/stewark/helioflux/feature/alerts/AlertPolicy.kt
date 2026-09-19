package ca.stewark.helioflux.feature.alerts

import kotlin.math.floor

data class AlertState(
    val lastKpBand: Int? = null,
    val notifiedFlareIds: Set<String> = emptySet(),
)

sealed interface AlertDecision {
    data class Geomagnetic(val band: Int, val escalation: Boolean) : AlertDecision
    data class Flare(val id: String, val flareClass: String) : AlertDecision
}

class AlertPolicy {
    fun evaluateKp(kp: Double?, state: AlertState): AlertDecision.Geomagnetic? {
        if (kp == null || kp < 5.0) return null
        val band = floor(kp).toInt()
        val previous = state.lastKpBand
        return if (previous == null || band > previous) {
            AlertDecision.Geomagnetic(band, previous != null)
        } else null
    }

    fun evaluateFlare(id: String, flareClass: String, state: AlertState): AlertDecision.Flare? {
        val severity = flareClass.trim().uppercase().firstOrNull()
        if (severity != 'M' && severity != 'X') return null
        if (id in state.notifiedFlareIds) return null
        return AlertDecision.Flare(id, flareClass)
    }
}
