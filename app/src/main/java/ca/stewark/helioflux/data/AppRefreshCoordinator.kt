package ca.stewark.helioflux.data

import ca.stewark.helioflux.RepositoryProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

interface AppRefreshCoordinatorProvider {
    val refreshCoordinator: AppRefreshCoordinator
}

class AppRefreshCoordinator(
    private val fastActions: List<suspend () -> Unit>,
    private val slowActions: List<suspend () -> Unit>,
    private val solarHeroAction: suspend () -> Unit,
) {
    private val startupStarted = AtomicBoolean(false)
    private val manualRefreshStarted = AtomicBoolean(false)
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    suspend fun refreshFast() {
        runActions(fastActions)
    }

    suspend fun refreshSlow() {
        runActions(slowActions)
    }

    suspend fun refreshAll() {
        if (!manualRefreshStarted.compareAndSet(false, true)) return
        _isRefreshing.value = true
        try {
            runActions(fastActions + slowActions + solarHeroAction)
        } finally {
            _isRefreshing.value = false
            manualRefreshStarted.set(false)
        }
    }

    fun refreshStartupOnce(scope: CoroutineScope) {
        if (!startupStarted.compareAndSet(false, true)) return
        scope.launch {
            runActions(fastActions + slowActions)
        }
    }

    private suspend fun runActions(actions: List<suspend () -> Unit>) {
        supervisorScope {
            actions.forEach { refresh ->
                launch {
                    runCatching { refresh() }
                }
            }
        }
    }

    companion object {
        internal const val startupRefreshIncludesSolarHero = false

        fun from(repositories: RepositoryProvider): AppRefreshCoordinator =
            AppRefreshCoordinator(
                fastActions = listOf(
                    repositories.spaceWeather::refreshMagnetic,
                    repositories.spaceWeather::refreshPlasma,
                    repositories.spaceWeather::refreshKp,
                    repositories.spaceWeather::refreshGoesMagnetometer,
                    repositories.spaceWeather::refreshHemisphericPower,
                    repositories.aurora::refresh,
                    repositories.solarActivity::refreshXray,
                    repositories.solarActivity::refreshAceEpam,
                ),
                slowActions = listOf(
                    repositories.forecast::refresh,
                    repositories.solarActivity::refreshFlareProbabilities,
                    repositories.solarActivity::refreshFlares,
                    repositories.solarActivity::refreshCmes,
                    repositories.solarImagery::refreshAll,
                ),
                solarHeroAction = repositories.solarHero::refresh,
            )
    }
}
