package ca.stewark.helioflux.feature.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.data.repository.SolarActivityRepository
import ca.stewark.helioflux.core.data.repository.SpaceWeatherRepository
import ca.stewark.helioflux.core.database.dao.AlertStateDao
import ca.stewark.helioflux.core.database.entity.AlertStateEntity
import ca.stewark.helioflux.core.model.FlareEvent
import ca.stewark.helioflux.core.model.KpSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class AlertFlare(val id: String, val flareClass: String)
data class AlertConditions(val kp: Double?, val flares: List<AlertFlare>)

class TransientAlertDataException(message: String) : Exception(message)

interface AlertRepository {
    suspend fun currentConditions(): AlertConditions
}

interface AlertStateStore {
    suspend fun read(): AlertState
    suspend fun write(state: AlertState)
}

interface AlertNotificationPoster {
    fun postGeomagnetic(band: Int, escalation: Boolean)
    fun postFlare(id: String, flareClass: String)
}

data class AlertWorkerDependencies(
    val repository: AlertRepository,
    val stateStore: AlertStateStore,
    val notificationPoster: AlertNotificationPoster,
)

interface AlertWorkerDependenciesProvider {
    fun alertWorkerDependencies(): AlertWorkerDependencies
}

class SpaceWeatherAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val dependenciesOverride: AlertWorkerDependencies? = null,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val dependencies = dependenciesOverride
            ?: (applicationContext as? AlertWorkerDependenciesProvider)?.alertWorkerDependencies()
            ?: return Result.failure()
        val conditions = try {
            dependencies.repository.currentConditions()
        } catch (_: TransientAlertDataException) {
            return Result.retry()
        }

        val policy = AlertPolicy()
        var state = dependencies.stateStore.read()

        policy.evaluateKp(conditions.kp, state)?.let { decision ->
            dependencies.notificationPoster.postGeomagnetic(decision.band, decision.escalation)
            state = state.copy(lastKpBand = decision.band)
            dependencies.stateStore.write(state)
        }

        conditions.flares.forEach { flare ->
            policy.evaluateFlare(flare.id, flare.flareClass, state)?.let { decision ->
                dependencies.notificationPoster.postFlare(decision.id, decision.flareClass)
                state = state.copy(notifiedFlareIds = state.notifiedFlareIds + decision.id)
                dependencies.stateStore.write(state)
            }
        }
        return Result.success()
    }
}

class RepositoryAlertRepository private constructor(
    private val kpStates: (Long, Long) -> Flow<RepositoryState<List<KpSample>>>,
    private val flareStates: () -> Flow<RepositoryState<List<FlareEvent>>>,
    private val nowMillis: () -> Long,
) : AlertRepository {
    constructor(
        spaceWeather: SpaceWeatherRepository,
        solarActivity: SolarActivityRepository,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) : this(spaceWeather::kp, solarActivity::flares, nowMillis)

    override suspend fun currentConditions(): AlertConditions {
        val now = nowMillis()
        val kpState = kpStates(now - DAY_MILLIS, now).first { it !is RepositoryState.Loading }
        val flareState = flareStates().first { it !is RepositoryState.Loading }

        if (kpState is RepositoryState.Failure) throw TransientAlertDataException(kpState.message)
        if (flareState is RepositoryState.Failure) throw TransientAlertDataException(flareState.message)

        val kp = (kpState as? RepositoryState.Available)?.data
            ?.maxByOrNull { it.timestampMillis }
            ?.kp
        val flares = (flareState as? RepositoryState.Available)?.data
            .orEmpty()
            .map { AlertFlare(it.id, it.flareClass) }
        return AlertConditions(kp, flares)
    }

    internal companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000

        fun forTest(
            kpStates: (Long, Long) -> Flow<RepositoryState<List<KpSample>>>,
            flareStates: () -> Flow<RepositoryState<List<FlareEvent>>>,
            nowMillis: () -> Long,
        ) = RepositoryAlertRepository(kpStates, flareStates, nowMillis)
    }
}

class RoomAlertStateStore(private val dao: AlertStateDao) : AlertStateStore {
    override suspend fun read(): AlertState {
        val entity = dao.get() ?: return AlertState()
        return AlertState(
            lastKpBand = entity.lastNotifiedKpBand,
            notifiedFlareIds = entity.notifiedFlareIds.lineSequence().filter(String::isNotBlank).toSet(),
        )
    }

    override suspend fun write(state: AlertState) {
        dao.update(
            AlertStateEntity(
                lastNotifiedKpBand = state.lastKpBand,
                notifiedFlareIds = state.notifiedFlareIds.sorted().joinToString("\n"),
            )
        )
    }
}

class AndroidAlertNotificationPoster(
    private val context: Context,
    private val formatter: SpaceWeatherNotificationFormatter = SpaceWeatherNotificationFormatter(),
) : AlertNotificationPoster {
    override fun postGeomagnetic(band: Int, escalation: Boolean) {
        post(GEOMAGNETIC_NOTIFICATION_ID, formatter.geomagnetic(band, escalation))
    }

    override fun postFlare(id: String, flareClass: String) {
        post(FLARE_NOTIFICATION_ID_BASE + (id.hashCode() and 0x0fffffff), formatter.flare(flareClass))
    }

    private fun post(id: Int, text: NotificationText) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, NotificationChannels.SPACE_WEATHER_ALERTS_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private companion object {
        const val GEOMAGNETIC_NOTIFICATION_ID = 1001
        const val FLARE_NOTIFICATION_ID_BASE = 2000
    }
}
