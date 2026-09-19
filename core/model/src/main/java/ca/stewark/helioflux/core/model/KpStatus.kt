package ca.stewark.helioflux.core.model

enum class KpStatus { Quiet, Unsettled, Minor, Moderate, Strong, Severe }

fun kpStatus(kp: Double): KpStatus = when {
    kp >= 9 -> KpStatus.Severe
    kp >= 7 -> KpStatus.Strong
    kp >= 6 -> KpStatus.Moderate
    kp >= 5 -> KpStatus.Minor
    kp >= 3 -> KpStatus.Unsettled
    else -> KpStatus.Quiet
}
