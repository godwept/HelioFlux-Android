package ca.stewark.helioflux.core.data.network

data class HttpResult(
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>>,
)

interface HttpTransport {
    suspend fun get(url: String): HttpResult
    suspend fun head(url: String): HttpResult
}
