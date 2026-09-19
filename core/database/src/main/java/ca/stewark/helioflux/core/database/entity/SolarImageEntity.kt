package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.stewark.helioflux.core.model.SolarImageType

@Entity(tableName = "solar_images")
data class SolarImageEntity(
    @PrimaryKey val type: SolarImageType,
    val sourceTimestampMillis: Long?,
    val url: String,
)
