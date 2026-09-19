package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.AceEpamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AceEpamDao {
    @Upsert suspend fun upsertAll(samples: List<AceEpamEntity>)
    @Query("SELECT * FROM ace_epam WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<AceEpamEntity>
    @Query("SELECT * FROM ace_epam WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis ASC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<AceEpamEntity>>
    @Query("DELETE FROM ace_epam WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
