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
import ca.stewark.helioflux.ui.theme.*

internal const val FORECAST_COLLAPSED_MAX_LINES = 5
internal const val FORECAST_CARD_MIN_HEIGHT_DP = 200

@Composable
fun ForecastCards(
    sections: List<ForecastSection>,
    modifier: Modifier = Modifier,
    expandedLayout: Boolean = false,
) {
    Column(modifier.testTag("forecast-section"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("NOAA Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(
            Modifier.fillMaxWidth().testTag("forecast-track"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(sections, key = { it.key }) { section -> ForecastCard(section, expandedLayout) }
        }
    }
}

@Composable
private fun ForecastCard(section: ForecastSection, expandedLayout: Boolean) {
    var expanded by rememberSaveable(section.key) { mutableStateOf(false) }
    val accent = forecastAccent(section.key)

    Card(
        Modifier
            .width(if (expandedLayout) 320.dp else 280.dp)
            .heightIn(min = FORECAST_CARD_MIN_HEIGHT_DP.dp)
            .clickable { expanded = !expanded }
            .testTag("forecast-" + section.key),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                section.title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                section.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else FORECAST_COLLAPSED_MAX_LINES,
            )
            Text(
                section.forecast,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else FORECAST_COLLAPSED_MAX_LINES,
            )
            Spacer(Modifier.weight(1f, fill = false))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                section.issueTime?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = SpaceMuted)
                }
                Text(
                    if (expanded) "TAP TO COLLAPSE" else "TAP TO EXPAND",
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
