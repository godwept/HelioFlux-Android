package ca.stewark.helioflux.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HelioFluxDarkColorScheme = darkColorScheme(
    primary = SolarOrange,
    onPrimary = SpaceBlack,
    secondary = DataCyan,
    tertiary = DataBlue,
    background = SpaceBlack,
    onBackground = SpaceOnSurface,
    surface = SpaceSurface,
    onSurface = SpaceOnSurface,
    surfaceVariant = SpaceSurfaceVariant,
    onSurfaceVariant = SpaceMuted,
    error = AlertRed,
)

@Composable
fun HelioFluxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HelioFluxDarkColorScheme,
        typography = HelioFluxTypography,
        content = content,
    )
}
