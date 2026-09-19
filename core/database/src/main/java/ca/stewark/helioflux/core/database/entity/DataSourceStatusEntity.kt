package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.stewark.helioflux.core.model.DataSourceKey

@Entity(tableName = "data_source_status")
data class DataSourceStatusEntity(
    @PrimaryKey val source: DataSourceKey,
    val observationTimestampMillis: Long?,
    val fetchedTimestampMillis: Long?,
    val lastSuccessTimestampMillis: Long?,
    val lastAttemptTimestampMillis: Long?,
    val lastErrorTimestampMillis: Long?,
    val lastErrorMessage: String?,
)
