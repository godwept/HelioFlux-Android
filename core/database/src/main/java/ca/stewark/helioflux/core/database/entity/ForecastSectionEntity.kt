package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecast_sections")
data class ForecastSectionEntity(
    @PrimaryKey val key: String,
    val title: String,
    val summary: String,
    val forecast: String,
    val issueTime: String?,
)
