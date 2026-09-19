package ca.stewark.helioflux.ui.spaceweather

fun <T> filterByTimeframe(
    samples: List<T>,
    timeframe: Timeframe,
    nowMillis: Long,
    timestampMillis: (T) -> Long,
): List<T> {
    val cutoff = nowMillis - timeframe.durationMillis
    return samples.filter { timestampMillis(it) in cutoff..nowMillis }
}
