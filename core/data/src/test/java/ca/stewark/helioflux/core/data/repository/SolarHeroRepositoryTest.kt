package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.helioviewer.HelioviewerApi
import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SolarHeroRepositoryTest {
 @Test fun cachedFramesEmitAndRefreshDeduplicates()=runTest{val dao=Frames(listOf(SolarHeroFrameEntity("old",1000,"old")));val status=Status();status.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,1000,1000,1000,1000,null,null));val now=2_000_000_000L;val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now))){now};assertEquals("old",(repo.frames().first() as RepositoryState.Available).data.single().url);repo.refresh();val rows=(repo.frames().first() as RepositoryState.Available).data;assertTrue(rows.isNotEmpty());assertEquals(rows.map{it.url}.distinct().size,rows.size)}
 @Test fun normalRefreshProducesOrderedMultiFrameAnimationSet()=runTest{val now=Instant.parse("2026-09-20T16:00:00Z").toEpochMilli();val dao=Frames(emptyList());val status=Status();val http=HistoricalHeroHttp();val repo=SolarHeroRepository(dao,status,HelioviewerApi(http)){now};repo.refresh();val rows=(repo.frames().first() as RepositoryState.Available).data;assertEquals(60,http.requestedDates.size);assertEquals(60,rows.size);assertEquals(60,rows.map{it.url}.distinct().size);assertEquals(rows.map{it.sourceTimestampMillis}.sorted(),rows.map{it.sourceTimestampMillis});assertEquals(Instant.ofEpochMilli(now-59*15*60_000L),http.requestedDates.first());assertEquals(Instant.ofEpochMilli(now),http.requestedDates.last())}
 @Test fun latestOnlyRefreshDoesNotMakeSingleFrameAnimationCacheFresh()=runTest{val now=Instant.parse("2026-09-20T16:00:00Z").toEpochMilli();val dao=Frames(emptyList());val status=Status();val http=HistoricalHeroHttp();val repo=SolarHeroRepository(dao,status,HelioviewerApi(http)){now};assertNotNull(repo.refreshLatest());assertEquals(1,dao.flow.value.size);repo.refreshIfStale();assertEquals(60,dao.flow.value.size);assertEquals(61,http.requestedDates.size)}
 @Test fun freshCacheSkipsRefreshButExpiredCacheRefreshes()=runTest{val now=2_000_000_000L;val freshDao=Frames(listOf(SolarHeroFrameEntity("old",now-1000,"old")));val freshStatus=Status();freshStatus.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,now-1000,now-1000,now-1000,now-1000,null,null));val freshRepo=SolarHeroRepository(freshDao,freshStatus,HelioviewerApi(HeroHttp(now))){now};freshRepo.refreshIfStale();assertEquals("old",freshDao.flow.value.single().imageId);val staleDao=Frames(listOf(SolarHeroFrameEntity("old",now-31*60*1000,"old")));val staleStatus=Status();staleStatus.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,now-31*60*1000,now-31*60*1000,now-31*60*1000,now-31*60*1000,null,null));val staleRepo=SolarHeroRepository(staleDao,staleStatus,HelioviewerApi(HeroHttp(now))){now};staleRepo.refreshIfStale();assertEquals(1,staleDao.flow.value.size);assertEquals("same",staleDao.flow.value.single().imageId)}
 @Test fun refreshLatestAppendsNewImageWithoutDiscardingAnimationFrames()=runTest{val now=2_000_000_000L;val dao=Frames(listOf(SolarHeroFrameEntity("old",now-1000,"old")));val status=Status();val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now))){now};assertNotNull(repo.refreshLatest());assertEquals(listOf("old","same"),dao.flow.value.map{it.imageId})}
 @Test fun refreshLatestSkipsUnchangedImageId()=runTest{val now=2_000_000_000L;val dao=Frames(listOf(SolarHeroFrameEntity("same",now,"old")));val status=Status();val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now))){now};assertNull(repo.refreshLatest());assertEquals(1,dao.flow.value.size)}
 @Test fun staleLatestReturnsFailureWithoutClearingCache()=runTest{val now=2_000_000_000L;val dao=Frames(listOf(SolarHeroFrameEntity("old",1000,"old")));val status=Status();status.upsert(DataSourceStatusEntity(SolarHeroRepository.SOURCE,1000,1000,1000,1000,null,null));val repo=SolarHeroRepository(dao,status,HelioviewerApi(HeroHttp(now-7*60*60*1000))){now};repo.refresh();val state=repo.frames().first() as RepositoryState.Failure;assertEquals("old",state.retainedData!!.single().url)}
 private class HeroHttp(private val date:Long):HttpTransport{override suspend fun get(url:String)=HttpResult(200,"{\"id\":\"same\",\"date\":\""+Instant.ofEpochMilli(date)+"\",\"name\":\"AIA\"}",emptyMap());override suspend fun head(url:String)=error("unused")}
 private class HistoricalHeroHttp:HttpTransport{
  val requestedDates=mutableListOf<Instant>()
  override suspend fun get(url:String):HttpResult{
   val rawDate=URI(url).rawQuery.split("&").first{it.startsWith("date=")}.substringAfter("=")
   val date=Instant.parse(URLDecoder.decode(rawDate,StandardCharsets.UTF_8.toString()))
   requestedDates+=date
   return HttpResult(200,"{\"id\":\"frame-${date.toEpochMilli()}\",\"date\":\"$date\",\"name\":\"AIA\"}",emptyMap())
  }
  override suspend fun head(url:String)=error("unused")
 }
 private class Frames(seed:List<SolarHeroFrameEntity>):SolarHeroFrameDao(){val flow=MutableStateFlow(seed);override suspend fun upsertAll(frames:List<SolarHeroFrameEntity>){flow.value=(flow.value.associateBy{it.imageId}+frames.associateBy{it.imageId}).values.sortedBy{it.sourceTimestampMillis}};override fun observeAll():Flow<List<SolarHeroFrameEntity>> = flow;override suspend fun latest()=flow.value.maxByOrNull{it.sourceTimestampMillis};override suspend fun deleteAll(){flow.value=emptyList()}}
 private class Status:DataSourceStatusDao{val m=mutableMapOf<DataSourceKey,MutableStateFlow<DataSourceStatusEntity?>>();override suspend fun upsert(status:DataSourceStatusEntity){m.getOrPut(status.source){MutableStateFlow(null)}.value=status};override suspend fun get(source:DataSourceKey)=m[source]?.value;override fun observe(source:DataSourceKey):Flow<DataSourceStatusEntity?> = m.getOrPut(source){MutableStateFlow(null)}}
}
