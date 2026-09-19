package ca.stewark.helioflux.ui.home
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import org.junit.Rule
import org.junit.Test

class HomeDashboardTest {
 @get:Rule val compose=createComposeRule()
 private val source=DataSourceKey("test")
 @Test fun cachedHeroAndConditionsRender(){
  val image=SolarImage(SolarImageType.Aia304,1,"https://example.test/sun.png")
  val state=HomeUiState(frames=RepositoryState.Failure(source,"offline",listOf(image),DataFreshness.Cached),kp=RepositoryState.Available(listOf(KpSample(1,5.0)),source,DataFreshness.Fresh))
  compose.setContent{HomeScreen(state,false,{})}
  compose.onNodeWithTag("solar-hero").assertExists()
  compose.onNodeWithText("Using cached solar imagery").assertExists()
  compose.onNodeWithTag("home-compact").assertExists()
  compose.onNodeWithText("5.0").assertExists()
 }
 @Test fun metricTapRoutesAndExpandedLayoutIsSideBySide(){
  var target:HelioFluxDestination?=null
  compose.setContent{HomeScreen(HomeUiState(),true,{target=it})}
  compose.onNodeWithTag("home-expanded").assertExists()
  compose.onNodeWithTag("metric-kp").performClick()
  compose.runOnIdle{assert(target==HelioFluxDestination.SpaceWeather)}
 }
 @Test fun forecastsExpandAndShowIssueText(){
  val section=ForecastSection("solar","Solar Activity","Summary","Forecast detail","Issued now")
  val state=HomeUiState(forecast=RepositoryState.Available(listOf(section),source,DataFreshness.Fresh))
  compose.setContent{HomeScreen(state,false,{})}
  compose.onNodeWithText("Summary").assertExists()
  compose.onNodeWithText("Forecast detail").assertDoesNotExist()
  compose.onNodeWithTag("forecast-solar").performClick()
  compose.onNodeWithText("Forecast detail").assertExists()
  compose.onNodeWithText("Issued now").assertExists()
 }
}
