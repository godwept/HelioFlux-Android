package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.EnlilFrameEntity

@Dao
interface EnlilFrameDao {
    @Upsert suspend fun upsertAll(frames: List<EnlilFrameEntity>)
    @Query("SELECT * FROM enlil_frames ORDER BY runTimestampMillis DESC, frameTimestampMillis ASC")
    suspend fun getOrdered(): List<EnlilFrameEntity>
    @Query("DELETE FROM enlil_frames WHERE runTimestampMillis != :runTimestampMillis")
    suspend fun deleteOtherRuns(runTimestampMillis: Long)
}
