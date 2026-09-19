package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.SolarImageEntity
import ca.stewark.helioflux.core.model.SolarImageType

@Dao
interface SolarImageDao {
    @Upsert suspend fun upsert(image: SolarImageEntity)
    @Query("SELECT * FROM solar_images WHERE type = :type LIMIT 1")
    suspend fun getLatest(type: SolarImageType): SolarImageEntity?
}
