package ca.stewark.helioflux.core.data.repository
import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SolarActivityRepositoryTest {
 @Test fun probabilitiesRetainPreviousDataOnFailure()=runTest{val f=Fakes();val r=f.repo();f.http.responses[HelioFluxEndpoints.flareProbabilities]="# Class C M X\nA 40 5 1 x";r.refreshFlareProbabilities();val ok=r.flareProbabilities().first() as RepositoryState.Available;assertEquals(40,ok.data.c);assertEquals(SolarActivityRepository.FLARE_PROBABILITIES,ok.source);f.http.responses[HelioFluxEndpoints.flareProbabilities]="bad";r.refreshFlareProbabilities();val bad=r.flareProbabilities().first() as RepositoryState.Failure;assertEquals(40,bad.retainedData?.c)}
 @Test fun xrayCombinesChannelsAndKeeps72Hours()=runTest{val f=Fakes();val n=1726747200000L;val r=f.repo(n);f.http.responses[HelioFluxEndpoints.goesPrimaryXray]="""[{"time_tag":"2024-09-19T11:00:00Z","satellite":18,"energy":"0.1-0.8nm","flux":2.0}]""";f.http.responses[HelioFluxEndpoints.goesSecondaryXray]="""[{"time_tag":"2024-09-19T11:00:00Z","satellite":19,"energy":"0.05-0.4nm","flux":1.0}]""";r.refreshXray();val d=(r.xray(n-72L*60*60*1000,n).first() as RepositoryState.Available).data.single();assertEquals(2.0,d.goes18Long!!,0.0);assertEquals(1.0,d.goes19Short!!,0.0)}
 @Test fun donkiQueriesAndValidEmptyArraysAreSuccess()=runTest{val f=Fakes();val n=1726747200000L;val r=f.repo(n);f.http.default="[]";r.refreshFlares();r.refreshCmes();assertTrue(f.http.urls.any{it.contains("/FLR?startDate=2024-09-16&endDate=2024-09-19")});assertTrue(f.http.urls.any{it.contains("/CMEAnalysis?")&&it.contains("mostAccurateOnly=true")});assertTrue(r.flares().first() is RepositoryState.Empty);assertTrue(r.cmes().first() is RepositoryState.Empty)}
 @Test fun donkiUpsertsStableIdsNewestFirst()=runTest{val f=Fakes();val r=f.repo(1726747200000L);f.http.default="""[{"flrID":"a","peakTime":"2024-09-19T10:00:00","classType":"M1"},{"flrID":"b","peakTime":"2024-09-19T11:00:00","classType":"X1"}]""";r.refreshFlares();assertEquals(listOf("b","a"),(r.flares().first() as RepositoryState.Available).data.map{it.id})}
 @Test fun aceArchiveRequestsAndKeepsFull72HourWindowWithCacheOnFailure()=runTest{val f=Fakes();val n=1726747200000L;val r=f.repo(n);val url=HelioFluxEndpoints.aceEpam(n-72L*60*60*1000,n);f.http.responses[url]="""time,p1,p3,p5,fp6p,p7
2024-09-16T11:55:00Z,1.0,2.0,3.0,0.3,0.03
2024-09-16T12:00:00Z,3.0,4.0,5.0,0.5,0.05
2024-09-18T12:00:00Z,8.0,9.0,10.0,1.0,0.1
2024-09-19T11:00:00Z,13.0,14.0,15.0,1.5,0.15""";r.refreshAceEpam();assertTrue(f.http.urls.contains(url));val d=(r.aceEpam(n-72L*60*60*1000,n).first() as RepositoryState.Available).data;assertEquals(3,d.size);assertEquals(1726488000000L,d.first().timestampMillis);assertEquals(0.5,d.first().protonFp6!!,0.0);assertEquals(0.15,d.last().protonP7!!,0.0);assertEquals(1726743600000L,d.last().timestampMillis);f.http.status=500;r.refreshAceEpam();assertEquals(3,(r.aceEpam(n-72L*60*60*1000,n).first() as RepositoryState.Failure).retainedData?.size)}

 private class Fakes{
  val x=Xray();val fl=Flare();val cm=Cme();val ac=Ace();val st=Status();val http=Http()
  fun repo(n:Long=3100)=SolarActivityRepository(x,fl,cm,ac,st,http){n}
 }
 private class Http:HttpTransport{val responses=mutableMapOf<String,String>();val urls=mutableListOf<String>();var default="";var status=200;override suspend fun get(url:String):HttpResult{urls+=url;return HttpResult(status,responses[url]?:default,emptyMap())};override suspend fun head(url:String)=HttpResult(200,"",emptyMap())}
 private class Status:DataSourceStatusDao{val m=mutableMapOf<DataSourceKey,MutableStateFlow<DataSourceStatusEntity?>>();override suspend fun upsert(status:DataSourceStatusEntity){m.getOrPut(status.source){MutableStateFlow(null)}.value=status};override suspend fun get(source:DataSourceKey)=m[source]?.value;override fun observe(source:DataSourceKey):Flow<DataSourceStatusEntity?> = m.getOrPut(source){MutableStateFlow(null)}}
 private class Xray:XrayFluxDao{val m=mutableMapOf<Long,XrayFluxEntity>();val flow=MutableStateFlow<List<XrayFluxEntity>>(emptyList());override suspend fun upsertAll(samples:List<XrayFluxEntity>){samples.forEach{m[it.timestampMillis]=it};flow.value=m.values.sortedBy{it.timestampMillis}};override suspend fun getBetween(a:Long,b:Long)=m.values.filter{it.timestampMillis in a..b}.sortedBy{it.timestampMillis};override fun observeBetween(a:Long,b:Long)=flow.map{it.filter{x->x.timestampMillis in a..b}};override suspend fun deleteBefore(c:Long){}}
 private class Ace:AceEpamDao{val m=mutableMapOf<Long,AceEpamEntity>();val flow=MutableStateFlow<List<AceEpamEntity>>(emptyList());override suspend fun upsertAll(samples:List<AceEpamEntity>){samples.forEach{m[it.timestampMillis]=it};flow.value=m.values.sortedBy{it.timestampMillis}};override suspend fun getBetween(a:Long,b:Long)=m.values.filter{it.timestampMillis in a..b}.sortedBy{it.timestampMillis};override fun observeBetween(a:Long,b:Long)=flow.map{it.filter{x->x.timestampMillis in a..b}};override suspend fun deleteBefore(c:Long){}}
 private class Flare:FlareEventDao{val m=mutableMapOf<String,FlareEventEntity>();val flow=MutableStateFlow<List<FlareEventEntity>>(emptyList());override suspend fun upsertAll(events:List<FlareEventEntity>){events.forEach{m[it.id]=it};flow.value=m.values.sortedByDescending{it.timestampMillis}};override suspend fun getNewestFirst()=flow.value;override fun observeNewestFirst():Flow<List<FlareEventEntity>> = flow;override suspend fun deleteBefore(c:Long){}}
 private class Cme:CmeEventDao{val m=mutableMapOf<String,CmeEventEntity>();val flow=MutableStateFlow<List<CmeEventEntity>>(emptyList());override suspend fun upsertAll(events:List<CmeEventEntity>){events.forEach{m[it.id]=it};flow.value=m.values.sortedByDescending{it.timestampMillis}};override suspend fun getNewestFirst()=flow.value;override fun observeNewestFirst():Flow<List<CmeEventEntity>> = flow;override suspend fun deleteBefore(c:Long){}}
}
