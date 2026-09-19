package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.ActiveRegionEntity

@Dao
abstract class ActiveRegionDao {
    @Query("SELECT * FROM active_regions ORDER BY id")
    abstract suspend fun getAll(): List<ActiveRegionEntity>
    @Query("DELETE FROM active_regions")
    protected abstract suspend fun deleteAll()
    @Upsert protected abstract suspend fun upsertAll(regions: List<ActiveRegionEntity>)

    @Transaction
    open suspend fun replaceAll(regions: List<ActiveRegionEntity>) {
        deleteAll()
        upsertAll(regions)
    }
}
