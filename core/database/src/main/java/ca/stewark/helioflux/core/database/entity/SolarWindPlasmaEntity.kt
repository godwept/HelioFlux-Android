package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "solar_wind_plasma", indices = [Index("timestampMillis")])
data class SolarWindPlasmaEntity(
    @PrimaryKey val timestampMillis: Long,
    val density: Double?,
    val speed: Double?,
    val temperature: Double?,
)
