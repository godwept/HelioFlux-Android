package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "hemispheric_power", indices = [Index("timestampMillis")])
data class HemisphericPowerEntity(@PrimaryKey val timestampMillis: Long, val north: Double?, val south: Double?)
