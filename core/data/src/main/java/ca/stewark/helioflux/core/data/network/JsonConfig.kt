package ca.stewark.helioflux.core.data.network

import kotlinx.serialization.json.Json

val helioFluxJson = Json {
    ignoreUnknownKeys = true
}
