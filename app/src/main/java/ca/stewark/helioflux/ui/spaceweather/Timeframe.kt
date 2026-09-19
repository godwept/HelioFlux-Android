package ca.stewark.helioflux.ui.spaceweather

enum class Timeframe(val durationMillis: Long) {
    OneHour(60L * 60L * 1_000L),
    ThreeHours(3L * 60L * 60L * 1_000L),
    TwelveHours(12L * 60L * 60L * 1_000L),
    TwoDays(2L * 24L * 60L * 60L * 1_000L),
}
