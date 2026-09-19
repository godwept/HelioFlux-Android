package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.SolarWindMagEntity

@Dao
interface SolarWindMagDao {
    @Upsert
    suspend fun upsertAll(samples: List<SolarWindMagEntity>)

    @Query("SELECT * FROM solar_wind_mag WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun range(startMillis: Long, endMillis: Long): List<SolarWindMagEntity>

    @Query("DELETE FROM solar_wind_mag WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long): Int
}
