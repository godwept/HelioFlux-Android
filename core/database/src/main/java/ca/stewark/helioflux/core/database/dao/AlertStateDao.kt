package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.AlertStateEntity

@Dao
interface AlertStateDao {
    @Query("SELECT * FROM alert_state WHERE id = 1")
    suspend fun get(): AlertStateEntity?

    @Upsert
    suspend fun update(state: AlertStateEntity)
}
