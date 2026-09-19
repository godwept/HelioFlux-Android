package ca.stewark.helioflux.ui.home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.ForecastSection
@Composable fun ForecastCards(sections:List<ForecastSection>,modifier:Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(10.dp)){Text("NOAA Forecast",style=MaterialTheme.typography.headlineSmall);sections.forEach{ForecastCard(it)}}}
@Composable private fun ForecastCard(section:ForecastSection){var expanded by rememberSaveable(section.key){mutableStateOf(false)};Card(Modifier.fillMaxWidth().clickable{expanded=!expanded}.testTag("forecast-"+section.key)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(section.title,style=MaterialTheme.typography.titleMedium);Text(section.summary);if(expanded)Text(section.forecast);section.issueTime?.let{Text(it,style=MaterialTheme.typography.labelSmall)}}}}
