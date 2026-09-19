package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.data.repository.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
data class SpaceWeatherUiState(
 val magnetic:RepositoryState<List<SolarWindMag>> = RepositoryState.Loading,
 val plasma:RepositoryState<List<SolarWindPlasma>> = RepositoryState.Loading,
 val kp:RepositoryState<List<KpSample>> = RepositoryState.Loading,
 val goesMagnetometer:RepositoryState<GoesMagnetometerSeries> = RepositoryState.Loading,
 val hemisphericPower:RepositoryState<List<HemisphericPowerSample>> = RepositoryState.Loading,
 val aurora:RepositoryState<AuroraSnapshot> = RepositoryState.Loading,
 val timeframe:Timeframe = Timeframe.ThreeHours,
){
 val currentBz get()=magnetic.dataOrNull()?.lastOrNull{it.bz!=null}?.bz
 val currentSpeed get()=plasma.dataOrNull()?.lastOrNull{it.speed!=null}?.speed
 val currentDensity get()=plasma.dataOrNull()?.lastOrNull{it.density!=null}?.density
 val freshness:Map<DataSourceKey,DataFreshness> get()=listOf(magnetic,plasma,kp,goesMagnetometer,hemisphericPower,aurora).mapNotNull{it.sourceAndFreshness()}.toMap()
}
private fun <T> RepositoryState<T>.dataOrNull():T?=when(this){is RepositoryState.Available->data;is RepositoryState.Failure->retainedData;else->null}
private fun RepositoryState<*>.sourceAndFreshness():Pair<DataSourceKey,DataFreshness>?=when(this){is RepositoryState.Available->source to freshness;is RepositoryState.Empty->source to freshness;is RepositoryState.Failure->freshness?.let{source to it};RepositoryState.Loading->null}
class SpaceWeatherViewModel private constructor(
 magnetic:Flow<RepositoryState<List<SolarWindMag>>>,plasma:Flow<RepositoryState<List<SolarWindPlasma>>>,kp:Flow<RepositoryState<List<KpSample>>>,
 goes:Flow<RepositoryState<GoesMagnetometerSeries>>,hemisphericPower:Flow<RepositoryState<List<HemisphericPowerSample>>>,aurora:Flow<RepositoryState<AuroraSnapshot>>,scope:CoroutineScope,
){
 private val selectedTimeframe=MutableStateFlow(Timeframe.ThreeHours)
 val state:StateFlow<SpaceWeatherUiState> = combine(magnetic,plasma,kp,goes,hemisphericPower,aurora,selectedTimeframe){v->
  @Suppress("UNCHECKED_CAST") SpaceWeatherUiState(v[0] as RepositoryState<List<SolarWindMag>>,v[1] as RepositoryState<List<SolarWindPlasma>>,v[2] as RepositoryState<List<KpSample>>,v[3] as RepositoryState<GoesMagnetometerSeries>,v[4] as RepositoryState<List<HemisphericPowerSample>>,v[5] as RepositoryState<AuroraSnapshot>,v[6] as Timeframe)
 }.stateIn(scope,SharingStarted.Eagerly,SpaceWeatherUiState())
 constructor(spaceWeather:SpaceWeatherRepository,auroraRepository:AuroraRepository,scope:CoroutineScope,nowMillis:()->Long=System::currentTimeMillis):this(
  spaceWeather.magnetic(nowMillis()-Timeframe.TwoDays.durationMillis,nowMillis()),spaceWeather.plasma(nowMillis()-Timeframe.TwoDays.durationMillis,nowMillis()),
  spaceWeather.kp(nowMillis()-Timeframe.TwoDays.durationMillis,nowMillis()),spaceWeather.goesMagnetometer(nowMillis()-Timeframe.TwoDays.durationMillis,nowMillis()),
  spaceWeather.hemisphericPower(nowMillis()-Timeframe.TwoDays.durationMillis,nowMillis()),auroraRepository.snapshot(),scope)
 fun selectTimeframe(timeframe:Timeframe){selectedTimeframe.value=timeframe}
 companion object{internal fun forTest(magnetic:Flow<RepositoryState<List<SolarWindMag>>>,plasma:Flow<RepositoryState<List<SolarWindPlasma>>>,kp:Flow<RepositoryState<List<KpSample>>>,goes:Flow<RepositoryState<GoesMagnetometerSeries>>,hemisphericPower:Flow<RepositoryState<List<HemisphericPowerSample>>>,aurora:Flow<RepositoryState<AuroraSnapshot>>,scope:CoroutineScope)=SpaceWeatherViewModel(magnetic,plasma,kp,goes,hemisphericPower,aurora,scope)}
}
