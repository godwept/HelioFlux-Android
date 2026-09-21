package ca.stewark.helioflux.core.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient(),
) : HttpTransport {
    override suspend fun get(url: String): HttpResult =
        withContext(Dispatchers.IO) {
            execute(Request.Builder().url(url).get().build())
        }

    override suspend fun head(url: String): HttpResult =
        withContext(Dispatchers.IO) {
            execute(Request.Builder().url(url).head().build())
        }

    private fun execute(request: Request): HttpResult =
        client.newCall(request).execute().use { response ->
            HttpResult(
                status = response.code,
                body = response.body.string(),
                headers = response.headers.toMultimap(),
            )
        }
}
