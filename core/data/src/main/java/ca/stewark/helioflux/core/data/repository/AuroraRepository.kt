package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.FreshnessEvaluator
import ca.stewark.helioflux.core.data.freshness.FreshnessPolicies
import ca.stewark.helioflux.core.data.freshness.FreshnessResult
import ca.stewark.helioflux.core.data.freshness.FreshnessSource
import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.data.parser.OvationParser
import ca.stewark.helioflux.core.database.dao.AuroraSnapshotDao
import ca.stewark.helioflux.core.database.RetentionPolicy
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.toDomain
import ca.stewark.helioflux.core.database.toEntities
import ca.stewark.helioflux.core.model.AuroraSnapshot
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuroraRepository(
    private val auroraDao: AuroraSnapshotDao,
    private val statusDao: DataSourceStatusDao,
    private val transport: HttpTransport,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun snapshot(): Flow<RepositoryState<AuroraSnapshot>> = statusDao.observe(SOURCE).map { status ->
        state(auroraDao.getLatestComplete()?.toDomain(), status)
    }

    suspend fun refresh() {
        val now = nowMillis()
        markAttempt(now)
        try {
            val response = transport.get(HelioFluxEndpoints.ovation)
            require(response.status in 200..299) { "HTTP ${response.status}" }
            val parsed = requireNotNull(OvationParser.parse(response.body)) { "OVATION response was invalid" }
            val (snapshot, points) = parsed.toEntities()
            auroraDao.replaceSnapshot(snapshot, points)
            auroraDao.deleteOld(now - RetentionPolicy.SOLAR_ACTIVITY_SERIES_MILLIS)
            statusDao.upsert(
                DataSourceStatusEntity(SOURCE, parsed.observationTimestampMillis, now, now, now, null, null)
            )
        } catch (error: Exception) {
            val previous = statusDao.get(SOURCE)
            statusDao.upsert(
                DataSourceStatusEntity(
                    SOURCE,
                    previous?.observationTimestampMillis,
                    previous?.fetchedTimestampMillis,
                    previous?.lastSuccessTimestampMillis,
                    now,
                    now,
                    error.message ?: "Refresh failed",
                )
            )
        }
    }

    private suspend fun markAttempt(now: Long) {
        val previous = statusDao.get(SOURCE)
        statusDao.upsert(previous?.copy(lastAttemptTimestampMillis = now)
            ?: DataSourceStatusEntity(SOURCE, null, null, null, now, null, null))
    }

    private fun state(data: AuroraSnapshot?, status: DataSourceStatusEntity?): RepositoryState<AuroraSnapshot> {
        val lastError = status?.lastErrorTimestampMillis
        val lastSuccess = status?.lastSuccessTimestampMillis
        if (lastError != null && (lastSuccess == null || lastError >= lastSuccess)) {
            return RepositoryState.Failure(
                SOURCE,
                status?.lastErrorMessage ?: "Refresh failed",
                data,
                DataFreshness.Cached.takeIf { data != null },
            )
        }
        if (data == null && lastSuccess == null) return RepositoryState.Loading
        if (data == null) return RepositoryState.Empty(SOURCE, DataFreshness.Cached)

        val freshness = when (val result = FreshnessEvaluator.evaluate(
            nowMillis(), status?.observationTimestampMillis, status?.fetchedTimestampMillis,
            lastSuccess != null, true, FreshnessPolicies.all.getValue(FreshnessSource.OVATION)
        )) {
            is FreshnessResult.Available -> result.freshness
            FreshnessResult.Unavailable -> DataFreshness.Cached
        }
        return RepositoryState.Available(data, SOURCE, freshness)
    }

    companion object {
        val SOURCE = DataSourceKey("noaa-ovation")
    }
}
