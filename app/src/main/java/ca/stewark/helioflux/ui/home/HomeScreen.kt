package ca.stewark.helioflux.ui.home
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
@Composable fun HomeScreen(state:HomeUiState,expanded:Boolean,onDestination:(HelioFluxDestination)->Unit,modifier:Modifier=Modifier){
 val forecasts=when(val f=state.forecast){is RepositoryState.Available->f.data;is RepositoryState.Failure->f.retainedData.orEmpty();else->emptyList()}
 LazyColumn(modifier.testTag("home-screen").padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{Text("HelioFlux",modifier=Modifier.padding(top=16.dp))}
  item{if(expanded)Row(Modifier.fillMaxWidth().testTag("home-expanded"),horizontalArrangement=Arrangement.spacedBy(16.dp)){SolarHero(state.frames,Modifier.weight(1f));CurrentConditions(state.conditions,onDestination,Modifier.weight(1f))}else Column(Modifier.fillMaxWidth().testTag("home-compact"),verticalArrangement=Arrangement.spacedBy(16.dp)){SolarHero(state.frames,Modifier.fillMaxWidth());CurrentConditions(state.conditions,onDestination,Modifier.fillMaxWidth())}}
  item{ForecastCards(forecasts,Modifier.fillMaxWidth().padding(bottom=24.dp))}
 }
}
