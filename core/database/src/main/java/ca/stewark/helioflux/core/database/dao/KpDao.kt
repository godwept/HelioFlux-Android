package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.KpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KpDao {
    @Upsert suspend fun upsertAll(samples: List<KpEntity>)

    @Query("SELECT * FROM kp WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<KpEntity>

    @Query("SELECT * FROM kp WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<KpEntity>>

    @Query("DELETE FROM kp WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
