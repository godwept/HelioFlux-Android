package ca.stewark.helioflux.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import ca.stewark.helioflux.ui.navigation.HelioFluxDestination

private const val BottomNavigationTag = "helioflux-bottom-navigation"
private const val NavigationRailTag = "helioflux-navigation-rail"

@Composable
fun HelioFluxApp() {
    HelioFluxApp(windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass)
}

@Composable
internal fun HelioFluxApp(windowSizeClass: WindowSizeClass) {
    var selectedRoute by rememberSaveable { mutableStateOf(HelioFluxDestination.Home.route) }
    val selectedDestination =
        HelioFluxDestination.entries.firstOrNull { it.route == selectedRoute }
            ?: HelioFluxDestination.Home
    val expanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowWidthSizeClass.EXPANDED.lowerBound)

    if (expanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.testTag(NavigationRailTag)) {
                HelioFluxDestination.entries.forEach { destination ->
                    NavigationRailItem(
                        selected = destination == selectedDestination,
                        onClick = { selectedRoute = destination.route },
                        icon = { Text(destination.label.take(1)) },
                        label = { Text(destination.label) },
                    )
                }
            }
            DestinationContent(
                destination = selectedDestination,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag(BottomNavigationTag)) {
                    HelioFluxDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == selectedDestination,
                            onClick = { selectedRoute = destination.route },
                            icon = { Text(destination.label.take(1)) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            DestinationContent(
                destination = selectedDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DestinationContent(
    destination: HelioFluxDestination,
    modifier: Modifier = Modifier,
) {
    Text(
        text = destination.label,
        modifier = modifier.testTag("destination-${destination.route}"),
    )
}
