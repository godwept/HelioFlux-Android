package ca.stewark.helioflux.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_state")
data class AlertStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastNotifiedKpBand: Int?,
    val notifiedFlareIds: String,
) {
    companion object { const val SINGLETON_ID = 1 }
}
