package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.SolarHeroFrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarHeroFrameDao {
    @Upsert suspend fun upsertAll(frames: List<SolarHeroFrameEntity>)
    @Query("SELECT * FROM solar_hero_frames ORDER BY sourceTimestampMillis ASC")
    fun observeAll(): Flow<List<SolarHeroFrameEntity>>
    @Query("SELECT * FROM solar_hero_frames ORDER BY sourceTimestampMillis DESC LIMIT 1")
    suspend fun latest(): SolarHeroFrameEntity?
    @Query("DELETE FROM solar_hero_frames")
    suspend fun deleteAll()
    @Transaction
    suspend fun replaceAll(frames: List<SolarHeroFrameEntity>) {
        deleteAll()
        upsertAll(frames)
    }
}
