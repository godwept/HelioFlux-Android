package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.HemisphericPowerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HemisphericPowerDao {
    @Upsert suspend fun upsertAll(samples: List<HemisphericPowerEntity>)

    @Query("SELECT * FROM hemispheric_power WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<HemisphericPowerEntity>

    @Query("SELECT * FROM hemispheric_power WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<HemisphericPowerEntity>>

    @Query("DELETE FROM hemispheric_power WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
