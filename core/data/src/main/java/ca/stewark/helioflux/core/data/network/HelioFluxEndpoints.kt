package ca.stewark.helioflux.core.data.network

object HelioFluxEndpoints {
    private const val NOAA = "https://services.swpc.noaa.gov"
    private const val WORKER = "https://helioflux-api-proxy.mathew-stewart.workers.dev/api"

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
    val aceEpam = "$NOAA/text/ace-epam.txt"
    val forecastDiscussion = "$NOAA/text/discussion.txt"
    val flareProbabilities = "$NOAA/text/3-day-solar-geomag-predictions.txt"

    val donki = "$WORKER/donki"
    val helioviewer = "$WORKER/helioviewer"
    val hek = "$WORKER/hek"
    val hmi = "$WORKER/hmi/"
    val lasco = "$WORKER/lasco/"
    val enlil = "$WORKER/enlil/"
}
