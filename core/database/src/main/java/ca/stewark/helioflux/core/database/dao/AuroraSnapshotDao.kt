package ca.stewark.helioflux.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import ca.stewark.helioflux.core.database.entity.AuroraPointEntity
import ca.stewark.helioflux.core.database.entity.AuroraSnapshotEntity

data class AuroraSnapshotWithPoints(
    val snapshot: AuroraSnapshotEntity,
    val points: List<AuroraPointEntity>,
)

@Dao
abstract class AuroraSnapshotDao {
    @Upsert protected abstract suspend fun upsertSnapshot(snapshot: AuroraSnapshotEntity)
    @Upsert protected abstract suspend fun upsertPoints(points: List<AuroraPointEntity>)
    @Query("DELETE FROM aurora_points WHERE snapshotTimestampMillis = :snapshotTimestampMillis")
    protected abstract suspend fun deletePointsForSnapshot(snapshotTimestampMillis: Long)
    @Query("SELECT * FROM aurora_snapshots ORDER BY observationTimestampMillis DESC LIMIT 1")
    protected abstract suspend fun latestSnapshot(): AuroraSnapshotEntity?
    @Query("SELECT * FROM aurora_points WHERE snapshotTimestampMillis = :snapshotTimestampMillis ORDER BY latitude ASC, longitude ASC")
    protected abstract suspend fun pointsForSnapshot(snapshotTimestampMillis: Long): List<AuroraPointEntity>
    @Query("DELETE FROM aurora_snapshots WHERE observationTimestampMillis < :cutoffMillis AND observationTimestampMillis != :keepTimestampMillis")
    protected abstract suspend fun deleteOldSnapshotsExcept(cutoffMillis: Long, keepTimestampMillis: Long)

    @Transaction
    open suspend fun replaceSnapshot(snapshot: AuroraSnapshotEntity, points: List<AuroraPointEntity>) {
        upsertSnapshot(snapshot)
        deletePointsForSnapshot(snapshot.observationTimestampMillis)
        upsertPoints(points)
    }

    @Transaction
    open suspend fun getLatestComplete(): AuroraSnapshotWithPoints? {
        val snapshot = latestSnapshot() ?: return null
        return AuroraSnapshotWithPoints(snapshot, pointsForSnapshot(snapshot.observationTimestampMillis))
    }

    @Transaction
    open suspend fun deleteOld(cutoffMillis: Long) {
        val latest = latestSnapshot() ?: return
        deleteOldSnapshotsExcept(cutoffMillis, latest.observationTimestampMillis)
    }
}
