package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.SolarWindPlasmaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarWindPlasmaDao {
    @Upsert
    suspend fun upsertAll(samples: List<SolarWindPlasmaEntity>)

    @Query("SELECT * FROM solar_wind_plasma WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun range(startMillis: Long, endMillis: Long): List<SolarWindPlasmaEntity>

    @Query("SELECT * FROM solar_wind_plasma WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeRange(startMillis: Long, endMillis: Long): Flow<List<SolarWindPlasmaEntity>>

    @Query("DELETE FROM solar_wind_plasma WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long): Int
}
