package ca.stewark.helioflux.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.window.core.layout.WindowSizeClass
import ca.stewark.helioflux.HelioFluxApplication
import ca.stewark.helioflux.ui.home.*
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import ca.stewark.helioflux.ui.spaceweather.*

private const val BottomNavigationTag="helioflux-bottom-navigation"
private const val NavigationRailTag="helioflux-navigation-rail"

@Composable fun HelioFluxApp(initialDestination:HelioFluxDestination?=null,initialFocus:String?=null){
 val app=LocalContext.current.applicationContext as HelioFluxApplication
 val scope=rememberCoroutineScope()
 val vm=remember(app){HomeViewModel(app.container.spaceWeather,app.container.solarActivity,app.container.forecast,app.container.solarHero,scope)}
 val homeState by vm.state.collectAsState()
 val spaceWeatherVm=remember(app){SpaceWeatherViewModel(app.container.spaceWeather,app.container.aurora,scope)}
 val spaceWeatherState by spaceWeatherVm.state.collectAsState()
 HelioFluxApp(currentWindowAdaptiveInfo().windowSizeClass,homeState,spaceWeatherState,spaceWeatherVm::selectTimeframe,initialDestination,initialFocus)
}

@Composable internal fun HelioFluxApp(windowSizeClass:WindowSizeClass,homeState:HomeUiState?=null,spaceWeatherState:SpaceWeatherUiState?=null,onTimeframe:(Timeframe)->Unit={},initialDestination:HelioFluxDestination?=null,initialFocus:String?=null){
 var selectedRoute by rememberSaveable{mutableStateOf((initialDestination?:HelioFluxDestination.Home).route)}
 val selected=HelioFluxDestination.entries.firstOrNull{it.route==selectedRoute}?:HelioFluxDestination.Home
 val expanded=windowSizeClass.isWidthAtLeastBreakpoint(840)
 val select:(HelioFluxDestination)->Unit={selectedRoute=it.route}
 if(expanded)Row(Modifier.fillMaxSize()){
  NavigationRail(Modifier.testTag(NavigationRailTag)){HelioFluxDestination.entries.forEach{d->NavigationRailItem(selected=d==selected,onClick={select(d)},icon={Text(d.label.take(1))},label={Text(d.label)})}}
  DestinationContent(selected,expanded,homeState,spaceWeatherState,onTimeframe,select,Modifier.fillMaxSize())
 }else Scaffold(bottomBar={NavigationBar(Modifier.testTag(BottomNavigationTag)){HelioFluxDestination.entries.forEach{d->NavigationBarItem(selected=d==selected,onClick={select(d)},icon={Text(d.label.take(1))},label={Text(d.label)})}}}){pad->
  DestinationContent(selected,false,homeState,spaceWeatherState,onTimeframe,select,Modifier.fillMaxSize().padding(pad))
 }
}
@Composable private fun DestinationContent(destination:HelioFluxDestination,expanded:Boolean,homeState:HomeUiState?,spaceWeatherState:SpaceWeatherUiState?,onTimeframe:(Timeframe)->Unit,onDestination:(HelioFluxDestination)->Unit,modifier:Modifier=Modifier){
 if(destination==HelioFluxDestination.Home&&homeState!=null)HomeScreen(homeState,expanded,onDestination,modifier)
 else if(destination==HelioFluxDestination.SpaceWeather&&spaceWeatherState!=null)SpaceWeatherScreen(spaceWeatherState,expanded,onTimeframe,modifier)
 else Text(destination.label,modifier.testTag("destination-"+destination.route))
}
