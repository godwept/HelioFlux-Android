package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goes_mag", indices = [Index("timestampMillis")])
data class GoesMagEntity(
    @PrimaryKey val timestampMillis: Long,
    val primary: Double?,
    val secondary: Double?,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
)
