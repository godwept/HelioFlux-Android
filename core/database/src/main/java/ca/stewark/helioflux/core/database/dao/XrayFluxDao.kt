package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.XrayFluxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XrayFluxDao {
    @Upsert suspend fun upsertAll(samples: List<XrayFluxEntity>)
    @Query("SELECT * FROM xray_flux WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<XrayFluxEntity>
    @Query("SELECT * FROM xray_flux WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<XrayFluxEntity>>
    @Query("DELETE FROM xray_flux WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
