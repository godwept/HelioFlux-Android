package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.FreshnessEvaluator
import ca.stewark.helioflux.core.data.freshness.FreshnessPolicies
import ca.stewark.helioflux.core.data.freshness.FreshnessResult
import ca.stewark.helioflux.core.data.freshness.FreshnessSource
import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpResult
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.data.parser.GoesMagnetometerParser
import ca.stewark.helioflux.core.data.parser.HemisphericPowerParser
import ca.stewark.helioflux.core.data.parser.KpParser
import ca.stewark.helioflux.core.data.parser.MagneticFieldParser
import ca.stewark.helioflux.core.data.parser.PlasmaParser
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.GoesMagDao
import ca.stewark.helioflux.core.database.dao.HemisphericPowerDao
import ca.stewark.helioflux.core.database.dao.KpDao
import ca.stewark.helioflux.core.database.dao.SolarWindMagDao
import ca.stewark.helioflux.core.database.dao.SolarWindPlasmaDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.GoesMagEntity
import ca.stewark.helioflux.core.database.toDomain
import ca.stewark.helioflux.core.database.toEntity
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.core.model.GoesMagSample
import ca.stewark.helioflux.core.model.HemisphericPowerSample
import ca.stewark.helioflux.core.model.KpSample
import ca.stewark.helioflux.core.model.SolarWindMag
import ca.stewark.helioflux.core.model.SolarWindPlasma
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class GoesMagnetometerSeries(
    val samples: List<GoesMagSample>,
    val primaryLabel: String?,
    val secondaryLabel: String?,
)

class SpaceWeatherRepository(
    private val magneticDao: SolarWindMagDao,
    private val plasmaDao: SolarWindPlasmaDao,
    private val kpDao: KpDao,
    private val goesMagDao: GoesMagDao,
    private val hemisphericPowerDao: HemisphericPowerDao,
    private val statusDao: DataSourceStatusDao,
    private val transport: HttpTransport,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun magnetic(startMillis: Long, endMillis: Long): Flow<RepositoryState<List<SolarWindMag>>> =
        combine(magneticDao.observeRange(startMillis, endMillis), statusDao.observe(MAGNETIC)) { rows, status ->
            state(rows.map { it.toDomain() }, status, MAGNETIC)
        }

    fun plasma(startMillis: Long, endMillis: Long): Flow<RepositoryState<List<SolarWindPlasma>>> =
        combine(plasmaDao.observeRange(startMillis, endMillis), statusDao.observe(PLASMA)) { rows, status ->
            state(rows.map { it.toDomain() }, status, PLASMA)
        }

    fun kp(startMillis: Long, endMillis: Long): Flow<RepositoryState<List<KpSample>>> =
        combine(kpDao.observeBetween(startMillis, endMillis), statusDao.observe(KP)) { rows, status ->
            state(rows.map { it.toDomain() }, status, KP)
        }

    fun goesMagnetometer(startMillis: Long, endMillis: Long): Flow<RepositoryState<GoesMagnetometerSeries>> =
        combine(goesMagDao.observeBetween(startMillis, endMillis), statusDao.observe(GOES_MAG)) { rows, status ->
            val series = GoesMagnetometerSeries(
                samples = rows.map { it.toDomain() },
                primaryLabel = rows.firstNotNullOfOrNull { it.primaryLabel },
                secondaryLabel = rows.firstNotNullOfOrNull { it.secondaryLabel },
            )
            state(series, series.samples.isNotEmpty(), status, GOES_MAG)
        }

    fun hemisphericPower(startMillis: Long, endMillis: Long): Flow<RepositoryState<List<HemisphericPowerSample>>> =
        combine(hemisphericPowerDao.observeBetween(startMillis, endMillis), statusDao.observe(HEMISPHERIC_POWER)) { rows, status ->
            state(rows.map { it.toDomain() }, status, HEMISPHERIC_POWER)
        }

    suspend fun refreshMagnetic() {
        refresh(
            source = MAGNETIC,
            request = { transport.get(HelioFluxEndpoints.rtswMag) },
            parse = MagneticFieldParser::parse,
            persist = { magneticDao.upsertAll(it.map(SolarWindMag::toEntity)) },
            observation = { it.maxOf { sample -> sample.timestampMillis } },
        )
    }

    suspend fun refreshPlasma() {
        refresh(
            source = PLASMA,
            request = { transport.get(HelioFluxEndpoints.rtswPlasma) },
            parse = PlasmaParser::parse,
            persist = { plasmaDao.upsertAll(it.map(SolarWindPlasma::toEntity)) },
            observation = { it.maxOf { sample -> sample.timestampMillis } },
        )
    }

    suspend fun refreshKp() {
        refresh(
            source = KP,
            request = { transport.get(HelioFluxEndpoints.kp) },
            parse = KpParser::parse,
            persist = { kpDao.upsertAll(it.map(KpSample::toEntity)) },
            observation = { it.maxOf { sample -> sample.timestampMillis } },
        )
    }

    suspend fun refreshGoesMagnetometer() {
        val now = nowMillis()
        markAttempt(GOES_MAG, now)
        try {
            val sources = transport.get(HelioFluxEndpoints.goesInstrumentSources).successfulBody()
            val primary = transport.get(HelioFluxEndpoints.goesPrimaryMagnetometer).successfulBody()
            val secondary = transport.get(HelioFluxEndpoints.goesSecondaryMagnetometer).successfulBody()
            val parsed = GoesMagnetometerParser.parse(sources, primary, secondary)
            require(parsed.data.isNotEmpty()) { "GOES magnetometer response contained no valid rows" }
            goesMagDao.upsertAll(parsed.data.map {
                GoesMagEntity(
                    timestampMillis = it.timestampMillis,
                    primary = it.primary,
                    secondary = it.secondary,
                    primaryLabel = parsed.primaryLabel,
                    secondaryLabel = parsed.secondaryLabel,
                )
            })
            markSuccess(GOES_MAG, parsed.data.maxOf { it.timestampMillis }, now)
        } catch (error: Exception) {
            markFailure(GOES_MAG, now, error)
        }
    }

    suspend fun refreshHemisphericPower() {
        refresh(
            source = HEMISPHERIC_POWER,
            request = { transport.get(HelioFluxEndpoints.hemisphericPower) },
            parse = HemisphericPowerParser::parse,
            persist = { hemisphericPowerDao.upsertAll(it.map(HemisphericPowerSample::toEntity)) },
            observation = { it.maxOf { sample -> sample.timestampMillis } },
        )
    }

    private suspend fun <T> refresh(
        source: DataSourceKey,
        request: suspend () -> HttpResult,
        parse: (String) -> List<T>,
        persist: suspend (List<T>) -> Unit,
        observation: (List<T>) -> Long,
    ) {
        val now = nowMillis()
        markAttempt(source, now)
        try {
            val parsed = parse(request().successfulBody())
            require(parsed.isNotEmpty()) { "${source.value} response contained no valid rows" }
            persist(parsed)
            markSuccess(source, observation(parsed), now)
        } catch (error: Exception) {
            markFailure(source, now, error)
        }
    }

    private suspend fun markAttempt(source: DataSourceKey, now: Long) {
        val previous = statusDao.get(source)
        statusDao.upsert(
            previous?.copy(lastAttemptTimestampMillis = now)
                ?: DataSourceStatusEntity(source, null, null, null, now, null, null)
        )
    }

    private suspend fun markSuccess(source: DataSourceKey, observation: Long, now: Long) {
        val previous = statusDao.get(source)
        statusDao.upsert(
            DataSourceStatusEntity(
                source = source,
                observationTimestampMillis = observation,
                fetchedTimestampMillis = now,
                lastSuccessTimestampMillis = now,
                lastAttemptTimestampMillis = now,
                lastErrorTimestampMillis = null,
                lastErrorMessage = null,
            )
        )
    }

    private suspend fun markFailure(source: DataSourceKey, now: Long, error: Exception) {
        val previous = statusDao.get(source)
        statusDao.upsert(
            DataSourceStatusEntity(
                source = source,
                observationTimestampMillis = previous?.observationTimestampMillis,
                fetchedTimestampMillis = previous?.fetchedTimestampMillis,
                lastSuccessTimestampMillis = previous?.lastSuccessTimestampMillis,
                lastAttemptTimestampMillis = now,
                lastErrorTimestampMillis = now,
                lastErrorMessage = error.message ?: error::class.simpleName ?: "Refresh failed",
            )
        )
    }

    private fun <T> state(
        data: List<T>,
        status: DataSourceStatusEntity?,
        source: DataSourceKey,
    ): RepositoryState<List<T>> = state(data, data.isNotEmpty(), status, source)

    private fun <T> state(
        data: T,
        hasData: Boolean,
        status: DataSourceStatusEntity?,
        source: DataSourceKey,
    ): RepositoryState<T> {
        val lastError = status?.lastErrorTimestampMillis
        val lastSuccess = status?.lastSuccessTimestampMillis
        val latestAttemptFailed = lastError != null && (lastSuccess == null || lastError >= lastSuccess)
        if (latestAttemptFailed) {
            return RepositoryState.Failure(
                source = source,
                message = status?.lastErrorMessage ?: "Refresh failed",
                retainedData = data.takeIf { hasData },
                freshness = DataFreshness.Cached.takeIf { hasData },
            )
        }
        if (!hasData && status?.lastSuccessTimestampMillis == null) return RepositoryState.Loading

        val freshness = when (
            val result = FreshnessEvaluator.evaluate(
                nowMillis = nowMillis(),
                observationTimestampMillis = status?.observationTimestampMillis,
                fetchedTimestampMillis = status?.fetchedTimestampMillis,
                refreshSucceeded = status?.lastSuccessTimestampMillis != null,
                hasCache = hasData || status?.lastSuccessTimestampMillis != null,
                policy = FreshnessPolicies.all.getValue(FreshnessSource.REALTIME_NOAA),
            )
        ) {
            is FreshnessResult.Available -> result.freshness
            FreshnessResult.Unavailable -> DataFreshness.Cached
        }
        return if (hasData) RepositoryState.Available(data, source, freshness)
        else RepositoryState.Empty(source, freshness)
    }

    private fun HttpResult.successfulBody(): String {
        require(status in 200..299) { "HTTP $status" }
        return body
    }

    companion object {
        val MAGNETIC = DataSourceKey("noaa-rtsw-magnetic")
        val PLASMA = DataSourceKey("noaa-rtsw-plasma")
        val KP = DataSourceKey("noaa-planetary-kp")
        val GOES_MAG = DataSourceKey("noaa-goes-magnetometer")
        val HEMISPHERIC_POWER = DataSourceKey("noaa-hemispheric-power")
    }
}
