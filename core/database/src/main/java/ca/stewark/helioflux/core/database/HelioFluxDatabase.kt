package ca.stewark.helioflux.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ca.stewark.helioflux.core.database.dao.AceEpamDao
import ca.stewark.helioflux.core.database.dao.CmeEventDao
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.FlareEventDao
import ca.stewark.helioflux.core.database.dao.ForecastSectionDao
import ca.stewark.helioflux.core.database.dao.GoesMagDao
import ca.stewark.helioflux.core.database.dao.HemisphericPowerDao
import ca.stewark.helioflux.core.database.dao.KpDao
import ca.stewark.helioflux.core.database.dao.XrayFluxDao
import ca.stewark.helioflux.core.database.dao.SolarWindMagDao
import ca.stewark.helioflux.core.database.dao.SolarWindPlasmaDao
import ca.stewark.helioflux.core.database.entity.AceEpamEntity
import ca.stewark.helioflux.core.database.entity.CmeEventEntity
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.FlareEventEntity
import ca.stewark.helioflux.core.database.entity.ForecastSectionEntity
import ca.stewark.helioflux.core.database.entity.GoesMagEntity
import ca.stewark.helioflux.core.database.entity.HemisphericPowerEntity
import ca.stewark.helioflux.core.database.entity.KpEntity
import ca.stewark.helioflux.core.database.entity.XrayFluxEntity
import ca.stewark.helioflux.core.database.entity.SolarWindMagEntity
import ca.stewark.helioflux.core.database.entity.SolarWindPlasmaEntity

@Database(
    entities = [
        DataSourceStatusEntity::class,
        SolarWindMagEntity::class,
        SolarWindPlasmaEntity::class,
        KpEntity::class,
        GoesMagEntity::class,
        HemisphericPowerEntity::class,
        XrayFluxEntity::class,
        AceEpamEntity::class,
        FlareEventEntity::class,
        CmeEventEntity::class,
        ForecastSectionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class HelioFluxDatabase : RoomDatabase() {
    abstract fun dataSourceStatusDao(): DataSourceStatusDao
    abstract fun solarWindMagDao(): SolarWindMagDao
    abstract fun solarWindPlasmaDao(): SolarWindPlasmaDao
    abstract fun kpDao(): KpDao
    abstract fun goesMagDao(): GoesMagDao
    abstract fun hemisphericPowerDao(): HemisphericPowerDao
    abstract fun xrayFluxDao(): XrayFluxDao
    abstract fun aceEpamDao(): AceEpamDao
    abstract fun flareEventDao(): FlareEventDao
    abstract fun cmeEventDao(): CmeEventDao
    abstract fun forecastSectionDao(): ForecastSectionDao
}
