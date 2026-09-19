package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.ForecastSectionEntity

@Dao
interface ForecastSectionDao {
    @Upsert suspend fun upsertAll(sections: List<ForecastSectionEntity>)
    @Query("SELECT * FROM forecast_sections ORDER BY CASE key WHEN 'solar' THEN 0 WHEN 'particle' THEN 1 WHEN 'wind' THEN 2 WHEN 'geospace' THEN 3 ELSE 4 END, key")
    suspend fun getAll(): List<ForecastSectionEntity>
}
