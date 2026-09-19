package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "flare_events", indices = [Index("timestampMillis")])
data class FlareEventEntity(
    @PrimaryKey val id: String,
    val flareClass: String,
    val timestampMillis: Long,
    val observatory: String,
    val region: String?,
    val location: String?,
)
