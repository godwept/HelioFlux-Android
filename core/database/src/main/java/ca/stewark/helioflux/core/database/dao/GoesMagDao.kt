package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.GoesMagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoesMagDao {
    @Upsert suspend fun upsertAll(samples: List<GoesMagEntity>)

    @Query("SELECT * FROM goes_mag WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<GoesMagEntity>

    @Query("SELECT * FROM goes_mag WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<GoesMagEntity>>

    @Query("DELETE FROM goes_mag WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
