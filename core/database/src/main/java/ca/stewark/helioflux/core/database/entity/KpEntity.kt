package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "kp", indices = [Index("timestampMillis")])
data class KpEntity(@PrimaryKey val timestampMillis: Long, val kp: Double?)
