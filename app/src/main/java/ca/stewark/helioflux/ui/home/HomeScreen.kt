package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination

@Composable
fun HomeScreen(
    state: HomeUiState,
    expanded: Boolean,
    onDestination: (HelioFluxDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forecasts = when (val f = state.forecast) {
        is RepositoryState.Available -> f.data
        is RepositoryState.Failure -> f.retainedData.orEmpty()
        else -> emptyList()
    }

    LazyColumn(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("home-screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "HELIOFLUX",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("home-masthead"),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }
        item {
            if (expanded) {
                Row(
                    Modifier.fillMaxWidth().testTag("home-expanded"),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SolarHero(state.frames, Modifier.weight(1f))
                    CurrentConditions(state.conditions, onDestination, Modifier.weight(1f))
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().testTag("home-compact"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SolarHero(state.frames, Modifier.fillMaxWidth())
                    CurrentConditions(state.conditions, onDestination, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            ForecastCards(
                sections = forecasts,
                expandedLayout = expanded,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}
