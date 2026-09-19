package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.helioviewer.HelioviewerApi
import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SolarHeroRepositoryTest {
 @Test fun cachedFramesEmitAndRefreshDeduplicates()=runTest{val dao=Frames(listOf(SolarHeroFrameEntity("old",1000,"old")));val status=Status();status.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,1000,1000,1000,1000,null,null));val now=2_000_000_000L;val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now))){now};assertEquals("old",(repo.frames().first() as RepositoryState.Available).data.single().url);repo.refresh();val rows=(repo.frames().first() as RepositoryState.Available).data;assertTrue(rows.isNotEmpty());assertEquals(rows.map{it.url}.distinct().size,rows.size)}
 @Test fun refreshLatestSkipsUnchangedImageId()=runTest{val now=2_000_000_000L;val dao=Frames(listOf(SolarHeroFrameEntity("same",now,"old")));val status=Status();val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now))){now};assertNull(repo.refreshLatest());assertEquals(1,dao.flow.value.size)}
 @Test fun staleLatestReturnsFailureWithoutClearingCache()=runTest{val now=2_000_000_000L;val dao=Frames(listOf(SolarHeroFrameEntity("old",1000,"old")));val status=Status();status.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,1000,1000,1000,1000,null,null));val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now-7*60*60*1000))){now};repo.refresh();val state=repo.frames().first() as RepositoryState.Failure;assertEquals("old",state.retainedData!!.single().url)}
 private class HeroHttp(private val date:Long):HttpTransport{override suspend fun get(url:String)=HttpResult(200,"{\"id\":\"same\",\"date\":\""+java.time.Instant.ofEpochMilli(date)+"\",\"name\":\"AIA\"}",emptyMap());override suspend fun head(url:String)=error("unused")}
 private class Frames(seed:List<SolarHeroFrameEntity>):SolarHeroFrameDao{val flow=MutableStateFlow(seed);override suspend fun upsertAll(frames:List<SolarHeroFrameEntity>){flow.value=frames};override fun observeAll():Flow<List<SolarHeroFrameEntity>> = flow;override suspend fun latest()=flow.value.maxByOrNull{it.sourceTimestampMillis};override suspend fun deleteAll(){flow.value=emptyList()}}
 private class Status:DataSourceStatusDao{val m=mutableMapOf<DataSourceKey,MutableStateFlow<DataSourceStatusEntity?>>();override suspend fun upsert(status:DataSourceStatusEntity){m.getOrPut(status.source){MutableStateFlow(null)}.value=status};override suspend fun get(source:DataSourceKey)=m[source]?.value;override fun observe(source:DataSourceKey):Flow<DataSourceStatusEntity?> = m.getOrPut(source){MutableStateFlow(null)}}
}
