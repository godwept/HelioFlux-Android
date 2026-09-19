package ca.stewark.helioflux.core.data.helioviewer

import ca.stewark.helioflux.core.data.network.OkHttpTransport
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class HelioviewerTest {
 @Test fun plannerSpansIntervalsDedupesAndRejectsStale(){val now=2_000_000_000L;val t=HelioviewerFramePlanner.sampleTimes(now);assertEquals(60,t.size);assertEquals(59*15*60_000L,t.last()-t.first());assertEquals(listOf("a","b"),HelioviewerFramePlanner.deduplicateById(listOf("a","a","b")){it});assertFalse(HelioviewerFramePlanner.isLatestUsable(now,now-HelioviewerFramePlanner.MAX_AGE_MILLIS-1))}
 @Test fun apiBuildsRequiredUrls()=runTest { MockWebServer().use { server ->
   server.enqueue(MockResponse().setBody("""{"id":"42","date":"2026-09-19 12:00:00","name":"AIA"}"""))
   val transport=object:ca.stewark.helioflux.core.data.network.HttpTransport {
    override suspend fun get(url:String):ca.stewark.helioflux.core.data.network.HttpResult { val suffix=url.substringAfter("/api/helioviewer");return OkHttpTransport(OkHttpClient()).get(server.url(suffix).toString()) }
    override suspend fun head(url:String)=throw UnsupportedOperationException()
   }
   val api=HelioviewerApi(transport);val image=api.getClosestImage("2026-09-19T12:00:00Z",13);assertEquals("42",image.id)
   val request=server.takeRequest();assertEquals("13",request.requestUrl!!.queryParameter("sourceId"));assertEquals("2026-09-19T12:00:00Z",request.requestUrl!!.queryParameter("date"))
   val download=api.downloadUrl("42");assertTrue(download.contains("width=512"));assertTrue(download.endsWith("type=png"))
 } }
}
