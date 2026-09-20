package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SolarImageryRepositoryTest {
 @Test fun sourcesPersistIndependentlyWhenOneFails()=runTest{val f=Fakes();val r=f.repo();f.http.headStatus[HelioFluxEndpoints.hmi]=500;f.http.getBody[HelioFluxEndpoints.hek]="""{"result":[{"ar_noaanum":"11234","hpc_x":10,"hpc_y":20}]}""";f.http.getBody[HelioFluxEndpoints.enlil]="""<a href="enlil_com2_1_20260919T120000.jpg">x</a>""";r.refreshAll();assertTrue(r.image(SolarImageType.Magnetogram).first() is RepositoryState.Failure);assertEquals(1,(r.regions().first() as RepositoryState.Available).data.size);assertEquals(1,(r.enlil().first() as RepositoryState.Available).data.size)}
 @Test fun refreshAllUsesWorkingLascoAnimationEndpoints()=runTest{val f=Fakes();val r=f.repo();r.refreshAll();assertNotNull(f.image.m[SolarImageType.LascoC2]?.value);assertEquals(HelioFluxEndpoints.lasco+"LATEST/current_c2.gif",f.image.m[SolarImageType.LascoC2]?.value?.url);assertEquals(HelioFluxEndpoints.lasco+"LATEST/current_c3.gif",f.image.m[SolarImageType.LascoC3]?.value?.url)}
 @Test fun headLastModifiedBecomesNormalizedTimestamp()=runTest{val f=Fakes();val r=f.repo();f.http.headers[HelioFluxEndpoints.hmi]=mapOf("Last-Modified" to listOf("Sat, 19 Sep 2026 12:00:00 GMT"));r.refreshImage(SolarImageType.Magnetogram,HelioFluxEndpoints.hmi);assertEquals(1789819200000L,(r.image(SolarImageType.Magnetogram).first() as RepositoryState.Available).data.sourceTimestampMillis)}
 private class Fakes{val image=Images();val region=Regions();val enlil=Enlil();val status=Status();val http=Http();fun repo()=SolarImageryRepository(image,region,enlil,status,http){1789820000000L}}
 private class Http:HttpTransport{val headStatus=mutableMapOf<String,Int>();val getBody=mutableMapOf<String,String>();val headers=mutableMapOf<String,Map<String,List<String>>>();override suspend fun head(url:String)=HttpResult(headStatus[url]?:200,"",headers[url]?:emptyMap());override suspend fun get(url:String)=HttpResult(200,getBody[url]?:"",emptyMap())}
 private class Images:SolarImageDao{val m=mutableMapOf<SolarImageType,MutableStateFlow<SolarImageEntity?>>();override suspend fun upsert(image:SolarImageEntity){m.getOrPut(image.type){MutableStateFlow(null)}.value=image};override suspend fun getLatest(type:SolarImageType)=m[type]?.value;override fun observeLatest(type:SolarImageType):Flow<SolarImageEntity?> = m.getOrPut(type){MutableStateFlow(null)}}
 private class Regions:ActiveRegionDao(){val flow=MutableStateFlow<List<ActiveRegionEntity>>(emptyList());override suspend fun getAll()=flow.value;override fun observeAll():Flow<List<ActiveRegionEntity>> = flow;override suspend fun deleteAll(){flow.value=emptyList()};override suspend fun upsertAll(regions:List<ActiveRegionEntity>){flow.value=regions}}
 private class Enlil:EnlilFrameDao{val flow=MutableStateFlow<List<EnlilFrameEntity>>(emptyList());override suspend fun upsertAll(frames:List<EnlilFrameEntity>){flow.value=frames};override suspend fun getOrdered()=flow.value;override fun observeOrdered():Flow<List<EnlilFrameEntity>> = flow;override suspend fun deleteOtherRuns(runTimestampMillis:Long){flow.value=flow.value.filter{it.runTimestampMillis==runTimestampMillis}}}
 private class Status:DataSourceStatusDao{val m=mutableMapOf<DataSourceKey,MutableStateFlow<DataSourceStatusEntity?>>();override suspend fun upsert(status:DataSourceStatusEntity){m.getOrPut(status.source){MutableStateFlow(null)}.value=status};override suspend fun get(source:DataSourceKey)=m[source]?.value;override fun observe(source:DataSourceKey):Flow<DataSourceStatusEntity?> = m.getOrPut(source){MutableStateFlow(null)}}
}
