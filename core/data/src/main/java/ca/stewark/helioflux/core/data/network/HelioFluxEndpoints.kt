package ca.stewark.helioflux.core.data.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object HelioFluxEndpoints {
    private const val NOAA = "https://services.swpc.noaa.gov"
    private const val LASP = "https://lasp.colorado.edu/space-weather-portal/latis/dap"
    private const val WORKER = "https://helioflux-api-proxy.mathew-stewart.workers.dev/api"
    private val hekTimestamp =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)

    val rtswMag = "$NOAA/json/rtsw/rtsw_mag_1m.json"
    val rtswPlasma = "$NOAA/json/rtsw/rtsw_wind_1m.json"
    val kp = "$NOAA/products/noaa-planetary-k-index.json"
    val goesInstrumentSources = "$NOAA/json/goes/instrument-sources.json"
    val goesPrimaryMagnetometer = "$NOAA/json/goes/primary/magnetometers-3-day.json"
    val goesSecondaryMagnetometer = "$NOAA/json/goes/secondary/magnetometers-3-day.json"
    val goesPrimaryXray = "$NOAA/json/goes/primary/xrays-3-day.json"
    val goesSecondaryXray = "$NOAA/json/goes/secondary/xrays-3-day.json"
    val ovation = "$NOAA/json/ovation_aurora_latest.json"
    val hemisphericPower = "$NOAA/text/aurora-nowcast-hemi-power.txt"
    val forecastDiscussion = "$NOAA/text/discussion.txt"
    val flareProbabilities = "$NOAA/text/3-day-solar-geomag-predictions.txt"

    fun aceEpam(startMillis: Long, endMillis: Long): String {
        val start = Instant.ofEpochMilli(startMillis)
        val end = Instant.ofEpochMilli(endMillis)
        return "$LASP/iswa_ace_epam_P5M.csv?time%3E=$start&time%3C=$end"
    }

    fun hmiMetadata(startMillis: Long, endMillis: Long): String {
        val start = Instant.ofEpochMilli(startMillis)
        val end = Instant.ofEpochMilli(endMillis)
        return "$LASP/iswa_sdo_aia_hmic_files.json?" +
            "time%3E=$start&time%3C=$end&takeRight(1)&project(time,url)"
    }

    fun hek(startMillis: Long, endMillis: Long): String {
        val start =
            URLEncoder.encode(
                hekTimestamp.format(Instant.ofEpochMilli(startMillis)),
                StandardCharsets.UTF_8.toString(),
            )
        val end =
            URLEncoder.encode(
                hekTimestamp.format(Instant.ofEpochMilli(endMillis)),
                StandardCharsets.UTF_8.toString(),
            )
        return "$WORKER/hek?" +
            "cosec=2&cmd=search&type=column&event_type=ar&event_coordsys=helioprojective" +
            "&event_starttime=$start&event_endtime=$end" +
            "&x1=-1200&x2=1200&y1=-1200&y2=1200"
    }

    val donki = "$WORKER/donki"
    val helioviewer = "$WORKER/helioviewer"
    val lasco = "$WORKER/lasco/"
    val enlil = "$WORKER/enlil/"
}
