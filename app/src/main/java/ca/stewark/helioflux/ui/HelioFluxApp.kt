package ca.stewark.helioflux.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import ca.stewark.helioflux.HelioFluxApplication
import ca.stewark.helioflux.ui.home.*
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import ca.stewark.helioflux.ui.solaractivity.*
import ca.stewark.helioflux.ui.spaceweather.*

private const val BottomNavigationTag="helioflux-bottom-navigation"
private const val NavigationRailTag="helioflux-navigation-rail"
private val NavBackground=Color(0xFF080B0F)
private val NavActive=Color(0xFFFF9F2A)
private val NavInactive=Color(0xFF8B949E)
private val NavDivider=Color(0x33FF9500)
private val NavAnimation=tween<androidx.compose.ui.unit.Dp>(durationMillis=225,easing=FastOutSlowInEasing)

@Composable fun HelioFluxApp(initialDestination:HelioFluxDestination?=null,initialFocus:String?=null){
 val app=LocalContext.current.applicationContext as HelioFluxApplication
 val scope=rememberCoroutineScope()
 val homeVm=remember(app){HomeViewModel(app.container.spaceWeather,app.container.solarActivity,app.container.forecast,app.container.solarHero,scope)}
 val homeState by homeVm.state.collectAsState()
 val spaceWeatherVm=remember(app){SpaceWeatherViewModel(app.container.spaceWeather,app.container.aurora,scope)}
 val spaceWeatherState by spaceWeatherVm.state.collectAsState()
 val solarActivityVm=remember(app){SolarActivityViewModel(app.container.solarActivity,app.container.solarImagery,scope)}
 val solarActivityState by solarActivityVm.state.collectAsState()
 HelioFluxApp(currentWindowAdaptiveInfo().windowSizeClass,homeState,spaceWeatherState,solarActivityState,spaceWeatherVm::selectTimeframe,initialDestination,initialFocus)
}

@Composable internal fun HelioFluxApp(windowSizeClass:WindowSizeClass,homeState:HomeUiState?=null,spaceWeatherState:SpaceWeatherUiState?=null,solarActivityState:SolarActivityUiState?=null,onTimeframe:(Timeframe)->Unit={},initialDestination:HelioFluxDestination?=null,initialFocus:String?=null){
 var selectedRoute by rememberSaveable{mutableStateOf((initialDestination?:HelioFluxDestination.Home).route)}
 val selected=HelioFluxDestination.entries.firstOrNull{it.route==selectedRoute}?:HelioFluxDestination.Home
 val expanded=windowSizeClass.isWidthAtLeastBreakpoint(840)
 val select:(HelioFluxDestination)->Unit={selectedRoute=it.route}
 if(expanded)Row(Modifier.fillMaxSize()){
  HelioFluxNavigationRail(selected,select)
  DestinationContent(selected,expanded,homeState,spaceWeatherState,solarActivityState,onTimeframe,select,Modifier.fillMaxSize())
 }else Scaffold(bottomBar={HelioFluxBottomNavigation(selected,select)}){pad->
  DestinationContent(selected,false,homeState,spaceWeatherState,solarActivityState,onTimeframe,select,Modifier.fillMaxSize().padding(pad))
 }
}

@Composable private fun HelioFluxBottomNavigation(selected:HelioFluxDestination,onSelect:(HelioFluxDestination)->Unit){
 val destinations=HelioFluxDestination.entries
 val selectedIndex=destinations.indexOf(selected)
 BoxWithConstraints(
  Modifier.fillMaxWidth().background(NavBackground).drawBehind{drawLine(NavDivider,Offset(0f,0f),Offset(size.width,0f),1f)}.testTag(BottomNavigationTag)
 ){
  val slot=maxWidth/destinations.size
  val target=slot*selectedIndex+slot/2-16.dp
  val indicatorOffset by animateDpAsState(target,NavAnimation,label="bottom-nav-indicator")
  Box(Modifier.offset(x=indicatorOffset).width(32.dp).height(3.dp).background(NavActive,CircleShape).testTag("helioflux-bottom-indicator"))
  Row(Modifier.fillMaxWidth().height(76.dp),verticalAlignment=Alignment.CenterVertically){
   destinations.forEach{destination->
    NavItem(destination,destination==selected,onSelect,Modifier.weight(1f))
   }
  }
 }
}

@Composable private fun HelioFluxNavigationRail(selected:HelioFluxDestination,onSelect:(HelioFluxDestination)->Unit){
 val destinations=HelioFluxDestination.entries
 val selectedIndex=destinations.indexOf(selected)
 val itemHeight=72.dp
 val target=itemHeight*selectedIndex+24.dp
 val indicatorOffset by animateDpAsState(target,NavAnimation,label="rail-nav-indicator")
 Box(Modifier.width(104.dp).fillMaxHeight().background(NavBackground).drawBehind{drawLine(NavDivider,Offset(size.width-1f,0f),Offset(size.width-1f,size.height),1f)}.testTag(NavigationRailTag)){
  Box(Modifier.offset(y=indicatorOffset).width(3.dp).height(24.dp).background(NavActive,CircleShape).testTag("helioflux-rail-indicator"))
  Column(Modifier.fillMaxWidth()){
   destinations.forEach{destination->
    NavItem(destination,destination==selected,onSelect,Modifier.fillMaxWidth().height(itemHeight))
   }
  }
 }
}

@Composable private fun NavItem(destination:HelioFluxDestination,selected:Boolean,onSelect:(HelioFluxDestination)->Unit,modifier:Modifier){
 val contentColor by animateColorAsState(if(selected)NavActive else NavInactive,tween(225),label="nav-color")
 Column(
  modifier.semantics(mergeDescendants=true){this.selected=selected}.testTag("nav-item-"+destination.route),
  horizontalAlignment=Alignment.CenterHorizontally,
  verticalArrangement=Arrangement.Center,
 ){
  IconButton(onClick={onSelect(destination)}){
   Icon(destinationIconModifier(destination,selected),contentDescription=null,tint=contentColor)
  }
  Text(destination.label,color=contentColor,fontSize=11.sp,maxLines=1)
 }
}

private fun destinationIconModifier(destination:HelioFluxDestination,selected:Boolean):Modifier =
 Modifier.size(25.dp).drawBehind{
  val stroke=Stroke(width=if(selected)2.2f else 1.8f)
  val c=Color.White
  when(destination){
   HelioFluxDestination.Home->{
    val p=Path().apply{moveTo(size.width*.16f,size.height*.48f);lineTo(size.width*.5f,size.height*.18f);lineTo(size.width*.84f,size.height*.48f);lineTo(size.width*.78f,size.height*.48f);lineTo(size.width*.78f,size.height*.82f);lineTo(size.width*.22f,size.height*.82f);lineTo(size.width*.22f,size.height*.48f);close()}
    drawPath(p,c,style=stroke)
   }
   HelioFluxDestination.SpaceWeather->{
    drawCircle(c,size.minDimension*.34f,style=stroke)
    drawOval(c,topLeft=Offset(size.width*.34f,size.height*.16f),size=androidx.compose.ui.geometry.Size(size.width*.32f,size.height*.68f),style=stroke)
    drawLine(c,Offset(size.width*.18f,size.height*.5f),Offset(size.width*.82f,size.height*.5f),stroke.width)
   }
   HelioFluxDestination.SolarActivity->{
    drawCircle(c,size.minDimension*.2f,style=stroke)
    repeat(8){i->
     val a=Math.toRadians((i*45).toDouble())
     val center=Offset(size.width/2,size.height/2)
     val r1=size.minDimension*.31f; val r2=size.minDimension*.43f
     drawLine(c,Offset(center.x+(kotlin.math.cos(a)*r1).toFloat(),center.y+(kotlin.math.sin(a)*r1).toFloat()),Offset(center.x+(kotlin.math.cos(a)*r2).toFloat(),center.y+(kotlin.math.sin(a)*r2).toFloat()),stroke.width)
    }
   }
  }
 }
@Composable private fun Icon(modifier:Modifier,contentDescription:String?,tint:Color){
 Box(modifier.drawBehind{
  if(tint==NavActive)drawCircle(NavActive.copy(alpha=.14f),radius=size.minDimension*.72f)
 }.then(Modifier))
}

@Composable private fun DestinationContent(destination:HelioFluxDestination,expanded:Boolean,homeState:HomeUiState?,spaceWeatherState:SpaceWeatherUiState?,solarActivityState:SolarActivityUiState?,onTimeframe:(Timeframe)->Unit,onDestination:(HelioFluxDestination)->Unit,modifier:Modifier=Modifier){
 when {
  destination==HelioFluxDestination.Home&&homeState!=null -> HomeScreen(homeState,expanded,onDestination,modifier)
  destination==HelioFluxDestination.SpaceWeather&&spaceWeatherState!=null -> SpaceWeatherScreen(spaceWeatherState,expanded,onTimeframe,modifier)
  destination==HelioFluxDestination.SolarActivity&&solarActivityState!=null -> SolarActivityScreen(solarActivityState,expanded)
  else -> Text(destination.label,modifier.testTag("destination-"+destination.route))
 }
}
