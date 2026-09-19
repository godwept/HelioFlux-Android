package ca.stewark.helioflux.ui.home

import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeConditions(val kp: Double?=null,val bz: Double?=null,val speed: Double?=null,val flare: FlareProbabilities?=null)
data class HomeUiState(
 val frames:RepositoryState<List<SolarImage>> = RepositoryState.Loading,
 val magnetic:RepositoryState<List<SolarWindMag>> = RepositoryState.Loading,
 val plasma:RepositoryState<List<SolarWindPlasma>> = RepositoryState.Loading,
 val kp:RepositoryState<List<KpSample>> = RepositoryState.Loading,
 val flare:RepositoryState<FlareProbabilities> = RepositoryState.Loading,
 val forecast:RepositoryState<List<ForecastSection>> = RepositoryState.Loading,
){
 val conditions get()=HomeConditions(kp.dataOrNull()?.lastOrNull()?.kp,magnetic.dataOrNull()?.lastOrNull{it.bz!=null}?.bz,plasma.dataOrNull()?.lastOrNull{it.speed!=null}?.speed,flare.dataOrNull())
}
private fun <T> RepositoryState<T>.dataOrNull():T?=when(this){is RepositoryState.Available->data;is RepositoryState.Failure->retainedData;else->null}
class HomeViewModel(private val spaceWeather:SpaceWeatherRepository,private val solarActivity:SolarActivityRepository,private val forecastRepository:ForecastRepository,private val solarHero:SolarHeroRepository,private val scope:CoroutineScope,nowMillis:()->Long=System::currentTimeMillis){
 private val end=nowMillis(); private val start=end-2L*24*60*60*1000
 val state:StateFlow<HomeUiState> = combine(solarHero.frames(),spaceWeather.magnetic(start,end),spaceWeather.plasma(start,end),spaceWeather.kp(start,end),solarActivity.flareProbabilities(),forecastRepository.sections()){v->
  @Suppress("UNCHECKED_CAST") HomeUiState(v[0] as RepositoryState<List<SolarImage>>,v[1] as RepositoryState<List<SolarWindMag>>,v[2] as RepositoryState<List<SolarWindPlasma>>,v[3] as RepositoryState<List<KpSample>>,v[4] as RepositoryState<FlareProbabilities>,v[5] as RepositoryState<List<ForecastSection>>)
 }.stateIn(scope,SharingStarted.Eagerly,HomeUiState())
 fun refresh(){scope.launch{solarHero.refresh()};scope.launch{spaceWeather.refreshMagnetic()};scope.launch{spaceWeather.refreshPlasma()};scope.launch{spaceWeather.refreshKp()};scope.launch{solarActivity.refreshFlareProbabilities()};scope.launch{forecastRepository.refresh()}}
}
