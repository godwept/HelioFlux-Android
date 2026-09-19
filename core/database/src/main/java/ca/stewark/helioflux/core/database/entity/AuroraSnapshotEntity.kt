package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aurora_snapshots")
data class AuroraSnapshotEntity(
    @PrimaryKey val observationTimestampMillis: Long,
    val forecastTimestampMillis: Long,
)
