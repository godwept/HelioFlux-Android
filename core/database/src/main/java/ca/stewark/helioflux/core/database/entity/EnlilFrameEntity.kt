package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "enlil_frames",
    primaryKeys = ["runTimestampMillis", "frameTimestampMillis"],
    indices = [Index("frameTimestampMillis")],
)
data class EnlilFrameEntity(
    val runTimestampMillis: Long,
    val frameTimestampMillis: Long,
    val url: String,
)
