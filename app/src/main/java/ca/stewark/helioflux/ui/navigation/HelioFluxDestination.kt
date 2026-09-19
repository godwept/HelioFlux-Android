package ca.stewark.helioflux.ui.navigation

enum class HelioFluxDestination(
    val route: String,
    val label: String,
) {
    Home(route = "home", label = "Home"),
    SpaceWeather(route = "space-weather", label = "Space Weather"),
    SolarActivity(route = "solar-activity", label = "Solar Activity"),
}
