package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cme_events", indices = [Index("timestampMillis")])
data class CmeEventEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val speed: Double?,
    val halfAngle: Double?,
    val direction: String?,
    val type: String?,
    val link: String?,
)
