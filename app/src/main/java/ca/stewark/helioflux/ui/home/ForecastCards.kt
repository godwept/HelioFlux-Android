package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.ForecastSection
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.FreshGreen
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface

internal const val FORECAST_COLLAPSED_MAX_LINES = 7
internal const val FORECAST_CARD_HEIGHT_DP = 260

@Composable
fun ForecastCards(
    sections: List<ForecastSection>,
    modifier: Modifier = Modifier,
    expandedLayout: Boolean = false,
) {
    Column(
        modifier.testTag("forecast-section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("NOAA Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(
            Modifier.fillMaxWidth().testTag("forecast-track"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sections, key = { it.key }) { section ->
                ForecastCard(section, expandedLayout)
            }
        }
    }
}

@Composable
private fun ForecastCard(section: ForecastSection, expandedLayout: Boolean) {
    var expanded by rememberSaveable(section.key) { mutableStateOf(false) }
    var forecastOverflows by remember(section.key, section.forecast) { mutableStateOf(false) }
    val accent = forecastAccent(section.key)
    val interaction = if (forecastOverflows || expanded) Modifier.clickable { expanded = !expanded } else Modifier

    Card(
        Modifier
            .width(if (expandedLayout) 320.dp else 280.dp)
            .height(FORECAST_CARD_HEIGHT_DP.dp)
            .then(interaction)
            .testTag("forecast-" + section.key),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                section.title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(section.summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                section.forecast,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else FORECAST_COLLAPSED_MAX_LINES,
                onTextLayout = { result ->
                    if (!expanded) forecastOverflows = result.hasVisualOverflow
                },
            )
            Spacer(Modifier.weight(1f))
            section.issueTime?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = SpaceMuted) }
            if (forecastOverflows || expanded) {
                Text(
                    if (expanded) "TAP TO COLLAPSE" else "TAP FOR FULL FORECAST",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpaceMuted,
                )
            }
        }
    }
}

private fun forecastAccent(key: String): Color = when {
    "particle" in key.lowercase() -> AlertRed
    "wind" in key.lowercase() -> DataCyan
    "geo" in key.lowercase() -> FreshGreen
    else -> SolarOrange
}
