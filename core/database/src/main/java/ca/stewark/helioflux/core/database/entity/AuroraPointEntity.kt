package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "aurora_points",
    primaryKeys = ["snapshotTimestampMillis", "latitude", "longitude"],
    foreignKeys = [ForeignKey(
        entity = AuroraSnapshotEntity::class,
        parentColumns = ["observationTimestampMillis"],
        childColumns = ["snapshotTimestampMillis"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("snapshotTimestampMillis")],
)
data class AuroraPointEntity(
    val snapshotTimestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val intensity: Double,
)
