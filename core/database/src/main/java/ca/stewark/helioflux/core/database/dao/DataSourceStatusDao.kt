package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.flow.Flow

@Dao
interface DataSourceStatusDao {
    @Upsert
    suspend fun upsert(status: DataSourceStatusEntity)

    @Query("SELECT * FROM data_source_status WHERE source = :source LIMIT 1")
    suspend fun get(source: DataSourceKey): DataSourceStatusEntity?

    @Query("SELECT * FROM data_source_status WHERE source = :source LIMIT 1")
    fun observe(source: DataSourceKey): Flow<DataSourceStatusEntity?>
}
