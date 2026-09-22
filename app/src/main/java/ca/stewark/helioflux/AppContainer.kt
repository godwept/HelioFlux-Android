package ca.stewark.helioflux

import android.content.Context
import androidx.room.Room
import ca.stewark.helioflux.core.data.helioviewer.HelioviewerApi
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.data.network.OkHttpTransport
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.database.HelioFluxDatabase
import ca.stewark.helioflux.imageloading.ImageDownloadProgressInterceptor
import ca.stewark.helioflux.imageloading.imageDownloadProgressRegistry
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

interface RepositoryProvider {
    val spaceWeather: SpaceWeatherRepository
    val aurora: AuroraRepository
    val forecast: ForecastRepository
    val solarActivity: SolarActivityRepository
    val solarImagery: SolarImageryRepository
    val solarHero: SolarHeroRepository
}

class AppContainer private constructor(
    val database: HelioFluxDatabase,
    val transport: HttpTransport,
    override val spaceWeather: SpaceWeatherRepository,
    override val aurora: AuroraRepository,
    override val forecast: ForecastRepository,
    override val solarActivity: SolarActivityRepository,
    override val solarImagery: SolarImageryRepository,
    override val solarHero: SolarHeroRepository,
    val imageLoader: ImageLoader,
) : RepositoryProvider {
    companion object {
        fun create(context: Context): AppContainer {
            val db=Room.databaseBuilder(context,HelioFluxDatabase::class.java,"helioflux.db").fallbackToDestructiveMigration().build()
            val transport=OkHttpTransport()
            val status=db.dataSourceStatusDao()
            val imageHttpClient =
                OkHttpClient.Builder()
                    .addInterceptor(ImageDownloadProgressInterceptor(imageDownloadProgressRegistry))
                    .build()
            val imageLoader =
                ImageLoader.Builder(context)
                    .components {
                        add(OkHttpNetworkFetcherFactory(callFactory = { imageHttpClient }))
                    }
                    .build()
            return AppContainer(
                db,transport,
                SpaceWeatherRepository(db.solarWindMagDao(),db.solarWindPlasmaDao(),db.kpDao(),db.goesMagDao(),db.hemisphericPowerDao(),status,transport),
                AuroraRepository(db.auroraSnapshotDao(),status,transport),
                ForecastRepository(db.forecastSectionDao(),status,transport),
                SolarActivityRepository(db.xrayFluxDao(),db.flareEventDao(),db.cmeEventDao(),db.aceEpamDao(),status,transport),
                SolarImageryRepository(db.solarImageDao(),db.activeRegionDao(),db.enlilFrameDao(),status,transport),
                SolarHeroRepository(db.solarHeroFrameDao(),status,HelioviewerApi(transport)),
                imageLoader,
            )
        }
    }
}
