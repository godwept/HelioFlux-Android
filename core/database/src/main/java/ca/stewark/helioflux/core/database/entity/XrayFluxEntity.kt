package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "xray_flux", indices = [Index("timestampMillis")])
data class XrayFluxEntity(@PrimaryKey val timestampMillis: Long, val goes18Short: Double?, val goes18Long: Double?, val goes19Short: Double?, val goes19Long: Double?)
