package ca.stewark.helioflux

import android.app.Application
import android.content.Context
import ca.stewark.helioflux.data.AppDataRefreshScheduler
import ca.stewark.helioflux.data.AppDataRefreshScheduling
import ca.stewark.helioflux.data.AppRefreshCoordinator
import ca.stewark.helioflux.data.AppRefreshCoordinatorProvider
import ca.stewark.helioflux.feature.alerts.AlertScheduler
import ca.stewark.helioflux.feature.alerts.AlertScheduling
import ca.stewark.helioflux.feature.alerts.AlertWorkerDependencies
import ca.stewark.helioflux.feature.alerts.AlertWorkerDependenciesProvider
import ca.stewark.helioflux.feature.alerts.AndroidAlertNotificationPoster
import ca.stewark.helioflux.feature.alerts.NotificationChannels
import ca.stewark.helioflux.feature.alerts.RepositoryAlertRepository
import ca.stewark.helioflux.feature.alerts.RoomAlertStateStore
import ca.stewark.helioflux.feature.widgets.SunWidgetDependenciesProvider
import ca.stewark.helioflux.feature.widgets.SunWidgetRefreshScheduler
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HelioFluxApplication :
    Application(),
    AlertWorkerDependenciesProvider,
    SunWidgetDependenciesProvider,
    AppRefreshCoordinatorProvider {
    lateinit var container: AppContainer
        private set

    override lateinit var refreshCoordinator: AppRefreshCoordinator
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var alertsInitialized = false
    private var dataRefreshInitialized = false

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)\n        SingletonImageLoader.setSafe { _ -> container.imageLoader }\n        refreshCoordinator = AppRefreshCoordinator.from(container)
        refreshCoordinator.refreshStartupOnce(applicationScope)
        initializeDataRefresh(AppDataRefreshScheduler(this))
        initializeAlerts(NotificationChannels::create, AlertScheduler(this))
        SunWidgetRefreshScheduler(this).schedule()
    }

    internal fun initializeDataRefresh(scheduler: AppDataRefreshScheduling) {
        if (dataRefreshInitialized) return
        scheduler.schedule()
        dataRefreshInitialized = true
    }

    internal fun initializeAlerts(
        channelCreator: (Context) -> Unit,
        scheduler: AlertScheduling,
    ) {
        if (alertsInitialized) return
        channelCreator(this)
        scheduler.schedule()
        alertsInitialized = true
    }

    override fun solarHeroRepository() = container.solarHero

    override fun alertWorkerDependencies(): AlertWorkerDependencies =
        AlertWorkerDependencies(
            repository = RepositoryAlertRepository(container.spaceWeather, container.solarActivity),
            stateStore = RoomAlertStateStore(container.database.alertStateDao()),
            notificationPoster = AndroidAlertNotificationPoster(this),
        )
}
