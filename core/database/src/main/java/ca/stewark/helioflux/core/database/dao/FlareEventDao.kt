package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.FlareEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlareEventDao {
    @Upsert suspend fun upsertAll(events: List<FlareEventEntity>)
    @Query("SELECT * FROM flare_events ORDER BY timestampMillis DESC")
    suspend fun getNewestFirst(): List<FlareEventEntity>
    @Query("SELECT * FROM flare_events ORDER BY timestampMillis DESC")
    fun observeNewestFirst(): Flow<List<FlareEventEntity>>
    @Query("DELETE FROM flare_events WHERE timestampMillis < :cutoffMillis")
    suspend fun deleteBefore(cutoffMillis: Long)
}
