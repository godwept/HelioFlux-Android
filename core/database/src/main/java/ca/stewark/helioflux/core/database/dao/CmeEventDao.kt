package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.CmeEventEntity

@Dao
interface CmeEventDao {
    @Upsert suspend fun upsertAll(events: List<CmeEventEntity>)
    @Query("SELECT * FROM cme_events ORDER BY timestampMillis DESC")
    suspend fun getNewestFirst(): List<CmeEventEntity>
    @Query("DELETE FROM cme_events WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
