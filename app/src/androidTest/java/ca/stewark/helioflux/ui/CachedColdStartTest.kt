package ca.stewark.helioflux.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ca.stewark.helioflux.core.data.helioviewer.HelioviewerApi
import ca.stewark.helioflux.core.data.network.HttpResult
import ca.stewark.helioflux.core.data.network.HttpTransport
import ca.stewark.helioflux.core.data.repository.ForecastRepository
import ca.stewark.helioflux.core.data.repository.SolarActivityRepository
import ca.stewark.helioflux.core.data.repository.SolarHeroRepository
import ca.stewark.helioflux.core.data.repository.SpaceWeatherRepository
import ca.stewark.helioflux.core.database.HelioFluxDatabase
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.KpEntity
import ca.stewark.helioflux.core.model.DataSourceKey
import ca.stewark.helioflux.ui.home.HomeScreen
import ca.stewark.helioflux.ui.home.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class CachedColdStartTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var database: HelioFluxDatabase

    @After fun closeDatabase() {
        if (::database.isInitialized) database.close()
    }

    @Test fun roomCacheRendersBeforeAnyNetworkRefresh() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, HelioFluxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val now = System.currentTimeMillis()
        runBlocking {
            database.kpDao().upsertAll(listOf(KpEntity(now - 1_000, 4.0)))
            database.dataSourceStatusDao().upsert(
                DataSourceStatusEntity(
                    source = SpaceWeatherRepository.KP,
                    observationTimestampMillis = now - 1_000,
                    fetchedTimestampMillis = now - 1_000,
                    lastSuccessTimestampMillis = now - 1_000,
                    lastAttemptTimestampMillis = now - 1_000,
                    lastErrorTimestampMillis = now,
                    lastErrorMessage = "offline",
                )
            )
        }
        val transport = object : HttpTransport {
            override suspend fun get(url: String): HttpResult = error("Network must not be used")
            override suspend fun head(url: String): HttpResult = error("Network must not be used")
        }
        val status = database.dataSourceStatusDao()
        val spaceWeather = SpaceWeatherRepository(database.solarWindMagDao(), database.solarWindPlasmaDao(), database.kpDao(), database.goesMagDao(), database.hemisphericPowerDao(), status, transport)
        val solarActivity = SolarActivityRepository(database.xrayFluxDao(), database.flareEventDao(), database.cmeEventDao(), database.aceEpamDao(), status, transport)
        val forecast = ForecastRepository(database.forecastSectionDao(), status, transport)
        val hero = SolarHeroRepository(database.solarHeroFrameDao(), status, HelioviewerApi(transport))

        rule.setContent {
            val scope = rememberCoroutineScope()
            val vm = remember { HomeViewModel(spaceWeather, solarActivity, forecast, hero, scope) }
            val state by vm.state.collectAsState()
            HomeScreen(state, expanded = false, onDestination = {})
        }

        rule.onNodeWithText("4.0").assertIsDisplayed()
    }
}
