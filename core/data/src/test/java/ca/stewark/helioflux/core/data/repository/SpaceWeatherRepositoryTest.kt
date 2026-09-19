package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.network.HelioFluxEndpoints
import ca.stewark.helioflux.core.data.network.HttpResult
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.GoesMagDao
import ca.stewark.helioflux.core.database.dao.HemisphericPowerDao
import ca.stewark.helioflux.core.database.dao.KpDao
import ca.stewark.helioflux.core.database.dao.SolarWindMagDao
import ca.stewark.helioflux.core.database.dao.SolarWindPlasmaDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.GoesMagEntity
import ca.stewark.helioflux.core.database.entity.HemisphericPowerEntity
import ca.stewark.helioflux.core.database.entity.KpEntity
import ca.stewark.helioflux.core.database.entity.SolarWindMagEntity
import ca.stewark.helioflux.core.database.entity.SolarWindPlasmaEntity
import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceWeatherRepositoryTest {
    private val magnetic = FakeMagDao()
    private val plasma = FakePlasmaDao()
    private val kp = FakeKpDao()
    private val goes = FakeGoesDao()
    private val power = FakePowerDao()
    private val status = FakeStatusDao()
    private val transport = FakeTransport()

    private val repository = SpaceWeatherRepository(
        magneticDao = magnetic,
        plasmaDao = plasma,
        kpDao = kp,
        goesMagDao = goes,
        hemisphericPowerDao = power,
        statusDao = status,
        transport = transport,
        nowMillis = { NOW },
    )

    @Test
    fun magneticEmitsCacheRefreshesAndPreservesCacheOnMalformedPayload() = runTest {
        magnetic.upsertAll(listOf(SolarWindMagEntity(NOW - 60_000, 1.0, 2.0, 3.0, 4.0)))

        val cached = repository.magnetic(0, Long.MAX_VALUE).first()
        assertTrue(cached is RepositoryState.Available)
        assertEquals(DataFreshness.Cached, (cached as RepositoryState.Available).freshness)

        transport[HelioFluxEndpoints.rtswMag] = ok(
            """[{"time_tag":"2026-09-19T12:00:00Z","bx_gsm":"5","by_gsm":"6","bz_gsm":"-7","bt":"8"}]"""
        )
        repository.refreshMagnetic()
        assertEquals(-7.0, magnetic.rows.value.last().bz!!, 0.0)
        assertNotNull(status.get(SpaceWeatherRepository.MAGNETIC)?.lastSuccessTimestampMillis)

        val beforeFailure = magnetic.rows.value
        transport[HelioFluxEndpoints.rtswMag] = ok("""{"not":"an array"}""")
        repository.refreshMagnetic()
        assertEquals(beforeFailure, magnetic.rows.value)
        assertNotNull(status.get(SpaceWeatherRepository.MAGNETIC)?.lastErrorTimestampMillis)
    }

    @Test
    fun plasmaRejectsZeroRowRefreshWithoutWipingCache() = runTest {
        plasma.upsertAll(listOf(SolarWindPlasmaEntity(NOW - 60_000, 5.0, 400.0, 100_000.0)))
        transport[HelioFluxEndpoints.rtswPlasma] = ok(
            """[{"time_tag":"2026-09-19T12:00:00Z","proton_density":"0","proton_speed":"0","proton_temperature":"1"}]"""
        )

        repository.refreshPlasma()

        assertEquals(1, plasma.rows.value.size)
        assertEquals(400.0, plasma.rows.value.single().speed!!, 0.0)
        assertNotNull(status.get(SpaceWeatherRepository.PLASMA)?.lastErrorTimestampMillis)
    }

    @Test
    fun kpRefreshPersistsValidDataAndMalformedPayloadPreservesCache() = runTest {
        transport[HelioFluxEndpoints.kp] = ok(
            """[{"time_tag":"2026-09-19T12:00:00Z","Kp":"5.33"}]"""
        )
        repository.refreshKp()
        assertEquals(5.33, kp.rows.value.single().kp!!, 0.0)

        val cached = kp.rows.value
        transport[HelioFluxEndpoints.kp] = ok("""{"bad":true}""")
        repository.refreshKp()

        assertEquals(cached, kp.rows.value)
        val state = repository.kp(0, Long.MAX_VALUE).first()
        assertTrue(state is RepositoryState.Failure)
        assertEquals(cached.size, (state as RepositoryState.Failure).retainedData?.size)
    }

    @Test
    fun goesLabelsAndArcjetNullGapsSurvivePersistence() = runTest {
        transport[HelioFluxEndpoints.goesInstrumentSources] =
            ok("""[{"magnetometers":{"primary":"19","secondary":"18"}}]""")
        transport[HelioFluxEndpoints.goesPrimaryMagnetometer] = ok(
            """[{"time_tag":"2026-09-19T12:00:00Z","Hp":100,"arcjet_flag":true},{"time_tag":"2026-09-19T12:01:00Z","Hp":101,"arcjet_flag":false}]"""
        )
        transport[HelioFluxEndpoints.goesSecondaryMagnetometer] = ok(
            """[{"time_tag":"2026-09-19T12:00:00Z","Hp":90,"arcjet_flag":false}]"""
        )

        repository.refreshGoesMagnetometer()

        val persisted = goes.rows.value
        assertEquals("GOES-19", persisted.first().primaryLabel)
        assertEquals("GOES-18", persisted.first().secondaryLabel)
        assertNull(persisted.first().primary)
        assertEquals(90.0, persisted.first().secondary!!, 0.0)

        val state = repository.goesMagnetometer(0, Long.MAX_VALUE).first()
        val series = (state as RepositoryState.Available).data
        assertEquals("GOES-19", series.primaryLabel)
        assertEquals("GOES-18", series.secondaryLabel)
        assertNull(series.samples.first().primary)
    }

    @Test
    fun invalidHemisphericPowerResponseDoesNotWipeHistory() = runTest {
        power.upsertAll(listOf(HemisphericPowerEntity(NOW - 60_000, 35.0, 31.0)))
        transport[HelioFluxEndpoints.hemisphericPower] = ok("not a valid NOAA power row")

        repository.refreshHemisphericPower()

        assertEquals(1, power.rows.value.size)
        assertEquals(35.0, power.rows.value.single().north, 0.0)
        assertNotNull(status.get(SpaceWeatherRepository.HEMISPHERIC_POWER)?.lastErrorTimestampMillis)
    }

    private fun ok(body: String) = HttpResult(200, body, emptyMap())

    private class FakeTransport : HttpTransport {
        private val responses = mutableMapOf<String, HttpResult>()
        operator fun set(url: String, result: HttpResult) { responses[url] = result }
        override suspend fun get(url: String) = responses[url] ?: HttpResult(500, "", emptyMap())
        override suspend fun head(url: String) = get(url)
    }

    private class FakeStatusDao : DataSourceStatusDao {
        private val values = mutableMapOf<DataSourceKey, MutableStateFlow<DataSourceStatusEntity?>>()
        private fun flow(source: DataSourceKey) = values.getOrPut(source) { MutableStateFlow(null) }
        override suspend fun upsert(status: DataSourceStatusEntity) { flow(status.source).value = status }
        override suspend fun get(source: DataSourceKey): DataSourceStatusEntity? = flow(source).value
        override fun observe(source: DataSourceKey): Flow<DataSourceStatusEntity?> = flow(source)
    }

    private class FakeMagDao : SolarWindMagDao {
        val rows = MutableStateFlow<List<SolarWindMagEntity>>(emptyList())
        override suspend fun upsertAll(samples: List<SolarWindMagEntity>) { rows.value = merge(rows.value, samples) { it.timestampMillis } }
        override suspend fun range(startMillis: Long, endMillis: Long) = rows.value.filter { it.timestampMillis in startMillis..endMillis }
        override fun observeRange(startMillis: Long, endMillis: Long): Flow<List<SolarWindMagEntity>> = rows
        override suspend fun deleteBefore(cutoffMillis: Long): Int {
            val old = rows.value
            rows.value = old.filter { it.timestampMillis >= cutoffMillis }
            return old.size - rows.value.size
        }
    }

    private class FakePlasmaDao : SolarWindPlasmaDao {
        val rows = MutableStateFlow<List<SolarWindPlasmaEntity>>(emptyList())
        override suspend fun upsertAll(samples: List<SolarWindPlasmaEntity>) { rows.value = merge(rows.value, samples) { it.timestampMillis } }
        override suspend fun range(startMillis: Long, endMillis: Long) = rows.value.filter { it.timestampMillis in startMillis..endMillis }
        override fun observeRange(startMillis: Long, endMillis: Long): Flow<List<SolarWindPlasmaEntity>> = rows
        override suspend fun deleteBefore(cutoffMillis: Long): Int {
            val old = rows.value
            rows.value = old.filter { it.timestampMillis >= cutoffMillis }
            return old.size - rows.value.size
        }
    }

    private class FakeKpDao : KpDao {
        val rows = MutableStateFlow<List<KpEntity>>(emptyList())
        override suspend fun upsertAll(samples: List<KpEntity>) { rows.value = merge(rows.value, samples) { it.timestampMillis } }
        override suspend fun getBetween(startMillis: Long, endMillis: Long) = rows.value.filter { it.timestampMillis in startMillis..endMillis }
        override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<KpEntity>> = rows
        override suspend fun deleteBefore(cutoffMillis: Long) { rows.value = rows.value.filter { it.timestampMillis >= cutoffMillis } }
    }

    private class FakeGoesDao : GoesMagDao {
        val rows = MutableStateFlow<List<GoesMagEntity>>(emptyList())
        override suspend fun upsertAll(samples: List<GoesMagEntity>) { rows.value = merge(rows.value, samples) { it.timestampMillis } }
        override suspend fun getBetween(startMillis: Long, endMillis: Long) = rows.value.filter { it.timestampMillis in startMillis..endMillis }
        override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<GoesMagEntity>> = rows
        override suspend fun deleteBefore(cutoffMillis: Long) { rows.value = rows.value.filter { it.timestampMillis >= cutoffMillis } }
    }

    private class FakePowerDao : HemisphericPowerDao {
        val rows = MutableStateFlow<List<HemisphericPowerEntity>>(emptyList())
        override suspend fun upsertAll(samples: List<HemisphericPowerEntity>) { rows.value = merge(rows.value, samples) { it.timestampMillis } }
        override suspend fun getBetween(startMillis: Long, endMillis: Long) = rows.value.filter { it.timestampMillis in startMillis..endMillis }
        override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<HemisphericPowerEntity>> = rows
        override suspend fun deleteBefore(cutoffMillis: Long) { rows.value = rows.value.filter { it.timestampMillis >= cutoffMillis } }
    }

    companion object {
        private const val NOW = 1_789_819_200_000L

        private fun <T> merge(old: List<T>, incoming: List<T>, timestamp: (T) -> Long): List<T> =
            (old + incoming).associateBy(timestamp).values.sortedBy(timestamp)
    }
}
