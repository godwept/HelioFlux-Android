package ca.stewark.helioflux.data

import ca.stewark.helioflux.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AppRefreshCoordinator(
    private val refreshActions: List<suspend () -> Unit>,
) {
    private val started = AtomicBoolean(false)

    fun refreshOnce(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        refreshActions.forEach { refresh ->
            scope.launch {
                runCatching { refresh() }
            }
        }
    }

    companion object {
        internal const val startupRefreshIncludesSolarHero = false

        fun from(repositories: RepositoryProvider): AppRefreshCoordinator =
            AppRefreshCoordinator(
                listOf(
                    repositories.spaceWeather::refreshMagnetic,
                    repositories.spaceWeather::refreshPlasma,
                    repositories.spaceWeather::refreshKp,
                    repositories.spaceWeather::refreshGoesMagnetometer,
                    repositories.spaceWeather::refreshHemisphericPower,
                    repositories.aurora::refresh,
                    repositories.forecast::refresh,
                    repositories.solarActivity::refreshFlareProbabilities,
                    repositories.solarActivity::refreshXray,
                    repositories.solarActivity::refreshFlares,
                    repositories.solarActivity::refreshCmes,
                    repositories.solarActivity::refreshAceEpam,
                    repositories.solarImagery::refreshAll,
                )
            )
    }
}
