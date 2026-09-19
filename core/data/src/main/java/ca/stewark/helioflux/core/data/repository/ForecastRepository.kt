package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.FreshnessEvaluator
import ca.stewark.helioflux.core.data.freshness.FreshnessPolicies
import ca.stewark.helioflux.core.data.freshness.FreshnessResult
import ca.stewark.helioflux.core.data.freshness.FreshnessSource
import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.data.parser.ForecastDiscussionParser
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.ForecastSectionDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.toDomain
import ca.stewark.helioflux.core.database.toEntity
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.core.model.ForecastSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ForecastRepository(
    private val forecastDao: ForecastSectionDao,
    private val statusDao: DataSourceStatusDao,
    private val transport: HttpTransport,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun sections(): Flow<RepositoryState<List<ForecastSection>>> = statusDao.observe(SOURCE).map { status ->
        state(forecastDao.getAll().map { it.toDomain() }, status)
    }

    suspend fun refresh() {
        val now = nowMillis()
        markAttempt(now)
        try {
            val response = transport.get(HelioFluxEndpoints.forecastDiscussion)
            require(response.status in 200..299) { "HTTP ${response.status}" }
            val parsed = ForecastDiscussionParser.parse(response.body)
            require(parsed.size == 4 && parsed.map { it.key }.toSet() == EXPECTED_KEYS) {
                "Forecast discussion did not contain all four sections"
            }
            forecastDao.upsertAll(parsed.map(ForecastSection::toEntity))
            statusDao.upsert(DataSourceStatusEntity(SOURCE, null, now, now, now, null, null))
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

    private fun state(data: List<ForecastSection>, status: DataSourceStatusEntity?): RepositoryState<List<ForecastSection>> {
        val lastError = status?.lastErrorTimestampMillis
        val lastSuccess = status?.lastSuccessTimestampMillis
        if (lastError != null && (lastSuccess == null || lastError >= lastSuccess)) {
            return RepositoryState.Failure(
                SOURCE,
                status?.lastErrorMessage ?: "Refresh failed",
                data.takeIf { it.isNotEmpty() },
                DataFreshness.Cached.takeIf { it.isNotEmpty() },
            )
        }
        if (data.isEmpty() && lastSuccess == null) return RepositoryState.Loading

        val freshness = when (val result = FreshnessEvaluator.evaluate(
            nowMillis(), status?.observationTimestampMillis, status?.fetchedTimestampMillis,
            lastSuccess != null, data.isNotEmpty() || lastSuccess != null,
            FreshnessPolicies.all.getValue(FreshnessSource.FORECAST_DISCUSSION)
        )) {
            is FreshnessResult.Available -> result.freshness
            FreshnessResult.Unavailable -> DataFreshness.Cached
        }
        return if (data.isEmpty()) RepositoryState.Empty(SOURCE, freshness)
        else RepositoryState.Available(data, SOURCE, freshness)
    }

    companion object {
        val SOURCE = DataSourceKey("noaa-forecast-discussion")
        private val EXPECTED_KEYS = setOf("solar", "particle", "wind", "geospace")
    }
}
