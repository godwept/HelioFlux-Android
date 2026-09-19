package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solar_hero_frames")
data class SolarHeroFrameEntity(
    @PrimaryKey val imageId: String,
    val sourceTimestampMillis: Long,
    val url: String,
)
