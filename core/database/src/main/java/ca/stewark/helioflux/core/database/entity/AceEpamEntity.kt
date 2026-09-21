package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ace_epam", indices = [Index("timestampMillis")])
data class AceEpamEntity(
    @PrimaryKey val timestampMillis: Long,
    val electronLow: Double?,
    val electronHigh: Double?,
    val protonLow: Double?,
    val protonMid: Double?,
    val protonHigh: Double?,
    val protonP7: Double?,
    val protonP8: Double?,
)
