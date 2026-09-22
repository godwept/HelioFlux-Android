package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*
import java.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SolarImageryRepositoryTest {
 private val now=Instant.parse("2026-09-22T14:00:00Z").toEpochMilli()
 private val imageTime=Instant.parse("2026-09-22T13:45:00Z").toEpochMilli()
 private val hmiMetadataUrl=HelioFluxEndpoints.hmiMetadata(now-24*60*60*1000L,now)
 private val hmibcUrl="https://iswa.ccmc.gsfc.nasa.gov/iswa_data_tree/observation/solar/sdo/hmi-magnetogram-color_2048x2048/2026/09/20260922_134500_2048_HMIBC.jpg"
 private val hmibcJson="""{"iswa_sdo_aia_hmic_files":{"samples":[{"timestamp":"2026-09-22 13:45:00.0","url":"$hmibcUrl"}]}}"""
 private val srsText=""":Product: Solar Region Summary
:Issued: 2026 Sep 22 0030 UTC
I.  Regions with Sunspots.  Locations Valid at 21/2400Z
Nmbr Location  Lo  Area  Z   LL   NN Mag Type
4533 S13E50   003  0040 Hsx  01   01 Alpha
4534 N11W21   074  0030 Dri  05   10 Beta
4535 N12E58   355  0030 Hrx  02   01 Alpha
4536 N03E01   052  0020 Cri  04   07 Beta
4537 N07W11   063  0010 Bxo  64   03 Beta
4538 N11W09   062  0010 Bxo  03   03 Beta
IA. H-alpha Plages without Spots.  Locations Valid at 21/2400Z Sep
Nmbr  Location  Lo
4532  S07W68   121
II. Regions Due to Return 22 Sep to 24 Sep
Nmbr Lat    Lo
None"""
 @Test fun sourcesPersistIndependentlyWhenOneFails()=runTest{val f=Fakes();val r=f.repo();f.http.getStatus[hmiMetadataUrl]=500;f.http.getBody[HelioFluxEndpoints.solarRegionSummary]=srsText;f.http.getBody[HelioFluxEndpoints.enlil]="""<a href="enlil_com2_1_20260919T120000.jpg">x</a>""";r.refreshAll();assertTrue(r.image(SolarImageType.Magnetogram).first() is RepositoryState.Failure);assertEquals(7,(r.regions().first() as RepositoryState.Available).data.size);assertEquals(1,(r.enlil().first() as RepositoryState.Available).data.size)}
 @Test fun refreshAllUsesWorkingLascoAnimationEndpoints()=runTest{val f=Fakes();val r=f.repo();f.http.getBody[hmiMetadataUrl]=hmibcJson;r.refreshAll();assertNotNull(f.image.m[SolarImageType.LascoC2]?.value);assertEquals(HelioFluxEndpoints.lasco+"LATEST/current_c2.gif",f.image.m[SolarImageType.LascoC2]?.value?.url);assertEquals(HelioFluxEndpoints.lasco+"LATEST/current_c3.gif",f.image.m[SolarImageType.LascoC3]?.value?.url)}
 @Test fun headLastModifiedBecomesNormalizedTimestamp()=runTest{val f=Fakes();val r=f.repo();val url=HelioFluxEndpoints.lasco+"LATEST/current_c2.gif";f.http.headers[url]=mapOf("Last-Modified" to listOf("Sat, 19 Sep 2026 12:00:00 GMT"));r.refreshImage(SolarImageType.LascoC2,url);assertEquals(1789819200000L,(r.image(SolarImageType.LascoC2).first() as RepositoryState.Available).data.sourceTimestampMillis)}
 @Test fun magnetogramRefreshUsesRecentLaTiSWindow()=runTest{val f=Fakes();val r=f.repo();f.http.getBody[hmiMetadataUrl]=hmibcJson;r.refreshMagnetogram();assertEquals(listOf(hmiMetadataUrl),f.http.requestedGets);assertTrue(r.image(SolarImageType.Magnetogram).first() is RepositoryState.Available)}
 @Test fun magnetogramRefreshPersistsLatestLaTiSHmibcSample()=runTest{val f=Fakes();val r=f.repo();f.http.getBody[hmiMetadataUrl]=hmibcJson;r.refreshMagnetogram();val image=(r.image(SolarImageType.Magnetogram).first() as RepositoryState.Available).data;assertEquals(imageTime,image.sourceTimestampMillis);assertEquals(hmibcUrl,image.url);assertEquals(imageTime,f.status.get(SolarImageryRepository.HMI)?.observationTimestampMillis)}
 @Test fun failedMagnetogramRefreshRetainsPreviousHmibcAsCached()=runTest{val f=Fakes();val r=f.repo();f.http.getBody[hmiMetadataUrl]=hmibcJson;r.refreshMagnetogram();val previous=f.image.m[SolarImageType.Magnetogram]?.value;f.http.getStatus[hmiMetadataUrl]=500;r.refreshMagnetogram();val state=r.image(SolarImageType.Magnetogram).first() as RepositoryState.Failure;assertEquals(previous?.url,state.retainedData?.url);assertEquals(previous?.sourceTimestampMillis,state.retainedData?.sourceTimestampMillis);assertEquals(DataFreshness.Cached,state.freshness);assertEquals(previous,f.image.m[SolarImageType.Magnetogram]?.value);assertFalse(f.http.requestedGets.any{it.contains("/api/hmi/")})}
 @Test fun refreshRegionsUsesCurrentSrsAndHmibcTimestamp()=runTest{val f=Fakes();val r=f.repo();f.image.upsert(SolarImageEntity(SolarImageType.Magnetogram,imageTime,hmibcUrl));f.http.getBody[HelioFluxEndpoints.solarRegionSummary]=srsText;r.refreshRegions();assertEquals(listOf(HelioFluxEndpoints.solarRegionSummary),f.http.requestedGets);assertFalse(f.http.requestedGets.any{it.contains("/api/hek")||it.contains("sunspot_report.json")});val regions=(r.regions().first() as RepositoryState.Available).data;assertEquals(setOf("4532","4533","4534","4535","4536","4537","4538"),regions.map{it.id}.toSet());assertEquals(-751.5,regions.single{it.id=="4535"}.helioprojectiveX,1.0);assertEquals(283.9,regions.single{it.id=="4538"}.helioprojectiveX,1.0)}
 private class Fakes{val image=Images();val region=Regions();val enlil=Enlil();val status=Status();val http=Http();fun repo()=SolarImageryRepository(image,region,enlil,status,http){Instant.parse("2026-09-22T14:00:00Z").toEpochMilli()}}
 private class Http:HttpTransport{val headStatus=mutableMapOf<String,Int>();val getStatus=mutableMapOf<String,Int>();val getBody=mutableMapOf<String,String>();val headers=mutableMapOf<String,Map<String,List<String>>>();val requestedGets=mutableListOf<String>();override suspend fun head(url:String)=HttpResult(headStatus[url]?:200,"",headers[url]?:emptyMap());override suspend fun get(url:String):HttpResult{requestedGets+=url;return HttpResult(getStatus[url]?:200,getBody[url]?:"",emptyMap())}}
 private class Images:SolarImageDao{val m=mutableMapOf<SolarImageType,MutableStateFlow<SolarImageEntity?>>();override suspend fun upsert(image:SolarImageEntity){m.getOrPut(image.type){MutableStateFlow(null)}.value=image};override suspend fun getLatest(type:SolarImageType)=m[type]?.value;override fun observeLatest(type:SolarImageType):Flow<SolarImageEntity?> = m.getOrPut(type){MutableStateFlow(null)}}
 private class Regions:ActiveRegionDao(){val flow=MutableStateFlow<List<ActiveRegionEntity>>(emptyList());override suspend fun getAll()=flow.value;override fun observeAll():Flow<List<ActiveRegionEntity>> = flow;override suspend fun deleteAll(){flow.value=emptyList()};override suspend fun upsertAll(regions:List<ActiveRegionEntity>){flow.value=regions}}
 private class Enlil:EnlilFrameDao{val flow=MutableStateFlow<List<EnlilFrameEntity>>(emptyList());override suspend fun upsertAll(frames:List<EnlilFrameEntity>){flow.value=frames};override suspend fun getOrdered()=flow.value;override fun observeOrdered():Flow<List<EnlilFrameEntity>> = flow;override suspend fun deleteOtherRuns(runTimestampMillis:Long){flow.value=flow.value.filter{it.runTimestampMillis==runTimestampMillis}}}
 private class Status:DataSourceStatusDao{val m=mutableMapOf<DataSourceKey,MutableStateFlow<DataSourceStatusEntity?>>();override suspend fun upsert(status:DataSourceStatusEntity){m.getOrPut(status.source){MutableStateFlow(null)}.value=status};override suspend fun get(source:DataSourceKey)=m[source]?.value;override fun observe(source:DataSourceKey):Flow<DataSourceStatusEntity?> = m.getOrPut(source){MutableStateFlow(null)}}
}
