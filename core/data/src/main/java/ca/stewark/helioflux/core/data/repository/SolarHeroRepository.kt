package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.*
import ca.stewark.helioflux.core.data.helioviewer.*
import ca.stewark.helioflux.core.database.dao.DataSourceStatusDao
import ca.stewark.helioflux.core.database.dao.SolarHeroFrameDao
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.entity.SolarHeroFrameEntity
import ca.stewark.helioflux.core.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class SolarHeroRepository(private val frameDao:SolarHeroFrameDao,private val statusDao:DataSourceStatusDao,private val api:HelioviewerApi,private val nowMillis:()->Long=System::currentTimeMillis){
 fun frames():Flow<RepositoryState<List<SolarImage>>> = combine(frameDao.observeAll(),statusDao.observe(SOURCE)){rows,status->state(rows.map{SolarImage(SolarImageType.Aia304,it.sourceTimestampMillis,it.url)},status)}
 suspend fun refreshIfStale(){val fetched=statusDao.get(SOURCE)?.fetchedTimestampMillis;val hasAnimationCache=frameDao.observeAll().first().map{it.url}.distinct().size>1;if(fetched!=null&&nowMillis()-fetched<CACHE_TTL_MILLIS&&hasAnimationCache)return;refresh()}
 suspend fun refreshLatest():SolarImage? {
  val now=nowMillis();attempt(now)
  return try {
   val image=api.getClosestImage(Instant.ofEpochMilli(now).toString());val timestamp=parseDate(image.date)
   require(HelioviewerFramePlanner.isLatestUsable(now,timestamp)){"Latest Helioviewer image is older than 6 hours"}
   if(frameDao.latest()?.imageId==image.id)return null
   val entity=SolarHeroFrameEntity(image.id,timestamp,api.downloadUrl(image.id));frameDao.upsertAll(listOf(entity));val previous=statusDao.get(SOURCE);statusDao.upsert(DataSourceStatusEntity(SOURCE,timestamp,previous?.fetchedTimestampMillis,now,now,null,null));SolarImage(SolarImageType.Aia304,timestamp,entity.url)
  } catch(e:Exception){val p=statusDao.get(SOURCE);statusDao.upsert(DataSourceStatusEntity(SOURCE,p?.observationTimestampMillis,p?.fetchedTimestampMillis,p?.lastSuccessTimestampMillis,now,now,e.message?:"Refresh failed"));throw e}
 }
 suspend fun refresh(){val now=nowMillis();attempt(now);try{val limiter=Semaphore(MAX_PARALLEL_REQUESTS);val images=coroutineScope{HelioviewerFramePlanner.sampleTimes(now).map{timestamp->async(Dispatchers.IO){limiter.withPermit{api.getClosestImage(Instant.ofEpochMilli(timestamp).toString())}}}.awaitAll()};val deduped=HelioviewerFramePlanner.deduplicateById(images){it.id};require(deduped.isNotEmpty()){"Helioviewer returned no frames"};val latest=deduped.maxOf{parseDate(it.date)};require(HelioviewerFramePlanner.isLatestUsable(now,latest)){"Latest Helioviewer image is older than 6 hours"};frameDao.replaceAll(deduped.map{SolarHeroFrameEntity(it.id,parseDate(it.date),api.downloadUrl(it.id))});statusDao.upsert(DataSourceStatusEntity(SOURCE,latest,now,now,now,null,null))}catch(e:Exception){val p=statusDao.get(SOURCE);statusDao.upsert(DataSourceStatusEntity(SOURCE,p?.observationTimestampMillis,p?.fetchedTimestampMillis,p?.lastSuccessTimestampMillis,now,now,e.message?:"Refresh failed"))}}
 private fun parseDate(value:String):Long=runCatching{Instant.parse(value).toEpochMilli()}.getOrElse{LocalDateTime.parse(value,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toInstant(ZoneOffset.UTC).toEpochMilli()}
 private suspend fun attempt(n:Long){val p=statusDao.get(SOURCE);statusDao.upsert(p?.copy(lastAttemptTimestampMillis=n)?:DataSourceStatusEntity(SOURCE,null,null,null,n,null,null))}
 private fun state(rows:List<SolarImage>,s:DataSourceStatusEntity?):RepositoryState<List<SolarImage>>{val er=s?.lastErrorTimestampMillis;val ok=s?.lastSuccessTimestampMillis;if(er!=null&&(ok==null||er>=ok))return RepositoryState.Failure(SOURCE,s.lastErrorMessage?:"Refresh failed",rows.takeIf{it.isNotEmpty()},DataFreshness.Cached.takeIf{rows.isNotEmpty()});if(rows.isEmpty()&&ok==null)return RepositoryState.Loading;val f=when(val r=FreshnessEvaluator.evaluate(nowMillis(),s?.observationTimestampMillis,s?.fetchedTimestampMillis,ok!=null,rows.isNotEmpty()||ok!=null,FreshnessPolicies.all.getValue(FreshnessSource.HELIOVIEWER))){is FreshnessResult.Available->r.freshness;FreshnessResult.Unavailable->DataFreshness.Cached};return if(rows.isEmpty())RepositoryState.Empty(SOURCE,f)else RepositoryState.Available(rows,SOURCE,f)}
 companion object{val SOURCE=DataSourceKey("helioviewer-aia304");private const val CACHE_TTL_MILLIS=30*60*1000L;private const val MAX_PARALLEL_REQUESTS=6}
}
