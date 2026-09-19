package ca.stewark.helioflux

import android.app.Application
import android.content.Context
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

class HelioFluxApplication : Application(), AlertWorkerDependenciesProvider, SunWidgetDependenciesProvider {
    lateinit var container: AppContainer
        private set

    private var alertsInitialized = false

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
        initializeAlerts(NotificationChannels::create, AlertScheduler(this))
        SunWidgetRefreshScheduler(this).schedule()
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
