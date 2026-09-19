package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "solar_wind_mag", indices = [Index("timestampMillis")])
data class SolarWindMagEntity(
    @PrimaryKey val timestampMillis: Long,
    val bx: Double?,
    val by: Double?,
    val bz: Double?,
    val bt: Double?,
)
