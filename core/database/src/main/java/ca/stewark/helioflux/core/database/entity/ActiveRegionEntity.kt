package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_regions")
data class ActiveRegionEntity(
    @PrimaryKey val id: String,
    val number: String?,
    val helioprojectiveX: Double,
    val helioprojectiveY: Double,
)
