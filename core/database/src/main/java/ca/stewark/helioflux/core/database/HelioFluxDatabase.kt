package ca.stewark.helioflux.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.SolarWindMagDao
import ca.stewark.helioflux.core.database.dao.SolarWindPlasmaDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.SolarWindMagEntity
import ca.stewark.helioflux.core.database.entity.SolarWindPlasmaEntity

@Database(
    entities = [
        DataSourceStatusEntity::class,
        SolarWindMagEntity::class,
        SolarWindPlasmaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class HelioFluxDatabase : RoomDatabase() {
    abstract fun dataSourceStatusDao(): DataSourceStatusDao
    abstract fun solarWindMagDao(): SolarWindMagDao
    abstract fun solarWindPlasmaDao(): SolarWindPlasmaDao
}
