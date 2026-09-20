package ca.stewark.helioflux.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination
import ca.stewark.helioflux.ui.theme.AlertRed
import ca.stewark.helioflux.ui.theme.DataCyan
import ca.stewark.helioflux.ui.theme.FreshGreen
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface
import java.util.Locale

@Composable
fun CurrentConditions(
    conditions: HomeConditions,
    onDestination: (HelioFluxDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.testTag("current-conditions"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Current Conditions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Kp", conditions.kp?.oneDecimal() ?: "—", SolarOrange, Modifier.weight(1f)) {
                onDestination(HelioFluxDestination.SpaceWeather)
            }
            Metric("Bz", conditions.bz?.let { it.oneDecimal() + " nT" } ?: "—", DataCyan, Modifier.weight(1f)) {
                onDestination(HelioFluxDestination.SpaceWeather)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("Wind", conditions.speed?.let { it.oneDecimal() + " km/s" } ?: "—", FreshGreen, Modifier.weight(1f)) {
                onDestination(HelioFluxDestination.SpaceWeather)
            }
            val flare = conditions.flare
            Metric("Flare", flare?.let { "M " + it.m + "% · X " + it.x + "%" } ?: "—", AlertRed, Modifier.weight(1f)) {
                onDestination(HelioFluxDestination.SolarActivity)
            }
        }
        val kp = conditions.kp
        Text(
            if (kp == null) "Aurora / geomagnetic status unavailable"
            else if (kp >= 5) "Geomagnetic storm conditions"
            else "Geomagnetic conditions below storm level",
            color = SpaceMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier.clickable(onClick = onClick).testTag("metric-" + label.lowercase()),
        color = SpaceSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Double.oneDecimal() = String.format(Locale.US, "%.1f", this)
