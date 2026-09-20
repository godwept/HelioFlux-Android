package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.R
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination

private val Orbitron = FontFamily(Font(R.font.orbitron_bold, FontWeight.Bold))

@Composable
private fun HelioFluxMasthead() {
    val titleStyle = TextStyle(
        fontFamily = Orbitron,
        fontSize = 30.4.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 6.08.sp,
        textAlign = TextAlign.Center,
    )
    Box(
        Modifier.fillMaxWidth().padding(top = 16.dp).testTag("home-masthead"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "HELIOFLUX",
            color = Color(0x59FF9500),
            style = titleStyle.copy(shadow = Shadow(Color(0x59FF9500), Offset.Zero, 28f)),
        )
        Text(
            "HELIOFLUX",
            color = Color(0xB3FF9500),
            style = titleStyle.copy(shadow = Shadow(Color(0xB3FF9500), Offset.Zero, 16f)),
        )
        Text(
            "HELIOFLUX",
            color = Color.Transparent,
            style = titleStyle.copy(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFA85200), Color(0xFFFF9F2A), Color(0xFFFFB347)),
                ),
            ),
        )
    }
}

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
        item { HelioFluxMasthead() }
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
