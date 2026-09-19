package ca.stewark.helioflux.core.data.helioviewer

object HelioviewerFramePlanner {
    const val FRAME_COUNT=60
    const val INTERVAL_MINUTES=15
    const val MAX_AGE_MILLIS=6L*60*60*1000

    fun sampleTimes(nowMillis:Long):List<Long> {
        val total=(FRAME_COUNT-1)*INTERVAL_MINUTES*60_000L
        return List(FRAME_COUNT) { i -> nowMillis-total+i*INTERVAL_MINUTES*60_000L }
    }

    fun <T> deduplicateById(frames:List<T>, id:(T)->String):List<T> {
        val seen=mutableSetOf<String>()
        return frames.filter { seen.add(id(it)) }
    }

    fun isLatestUsable(nowMillis:Long, latestMillis:Long):Boolean = nowMillis-latestMillis <= MAX_AGE_MILLIS
}
