package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.*
import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.data.parser.ActiveRegionParser
import ca.stewark.helioflux.core.data.parser.EnlilListingParser
import ca.stewark.helioflux.core.data.parser.HmibcMetadataParser
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.toDomain
import ca.stewark.helioflux.core.database.toEntity
import ca.stewark.helioflux.core.model.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SolarImageryRepository(private val imageDao:SolarImageDao,private val regionDao:ActiveRegionDao,private val enlilDao:EnlilFrameDao,private val statusDao:DataSourceStatusDao,private val transport:HttpTransport,private val nowMillis:()->Long=System::currentTimeMillis){
 fun image(type:SolarImageType):Flow<RepositoryState<SolarImage>> = combine(imageDao.observeLatest(type),statusDao.observe(sourceFor(type))){row,status->state(row?.toDomain(),row!=null,status,sourceFor(type))}
 fun regions()=combine(regionDao.observeAll(),statusDao.observe(HEK)){rows,status->state(rows.map{it.toDomain()},rows.isNotEmpty(),status,HEK)}
 fun enlil()=combine(enlilDao.observeOrdered(),statusDao.observe(ENLIL)){rows,status->state(rows.map{it.toDomain()},rows.isNotEmpty(),status,ENLIL)}
 suspend fun refreshAll(){refreshMagnetogram();refreshImage(SolarImageType.LascoC2,HelioFluxEndpoints.lasco+"LATEST/current_c2.gif");refreshImage(SolarImageType.LascoC3,HelioFluxEndpoints.lasco+"LATEST/current_c3.gif");refreshRegions();refreshEnlil()}
 suspend fun refreshMagnetogram(){val now=nowMillis();attempt(HMI,now);try{val url=HelioFluxEndpoints.hmiMetadata(now-HMI_LOOKBACK_MILLIS,now);val response=transport.get(url);require(response.status in 200..299){"HTTP "+response.status};val record=requireNotNull(HmibcMetadataParser.parse(response.body)){"LaTiS HMIBC response contained no valid image"};imageDao.upsert(SolarImage(SolarImageType.Magnetogram,record.timestampMillis,record.url).toEntity());success(HMI,record.timestampMillis,now)}catch(e:Exception){fail(HMI,now,e)}}
 suspend fun refreshImage(type:SolarImageType,url:String){val key=sourceFor(type);val now=nowMillis();attempt(key,now);try{val response=transport.head(url);require(response.status in 200..299){"HTTP "+response.status};val stamp=lastModified(response)?:now;imageDao.upsert(SolarImage(type,stamp,url).toEntity());success(key,stamp,now)}catch(e:Exception){fail(key,now,e)}}
 suspend fun refreshRegions(){val now=nowMillis();attempt(HEK,now);try{val url=HelioFluxEndpoints.hek(now-ACTIVE_REGION_LOOKBACK_MILLIS,now);val r=transport.get(url);require(r.status in 200..299){"HTTP "+r.status};val rows=ActiveRegionParser.parse(r.body);regionDao.replaceAll(rows.map(ActiveRegion::toEntity));success(HEK,now,now)}catch(e:Exception){fail(HEK,now,e)}}
 suspend fun refreshEnlil(){val now=nowMillis();attempt(ENLIL,now);try{val r=transport.get(HelioFluxEndpoints.enlil);require(r.status in 200..299){"HTTP "+r.status};val rows=EnlilListingParser.parse(r.body,HelioFluxEndpoints.enlil);require(rows.isNotEmpty()){"ENLIL listing contained no frames"};enlilDao.upsertAll(rows.map(EnlilFrame::toEntity));enlilDao.deleteOtherRuns(rows.first().runTimestampMillis);success(ENLIL,rows.first().runTimestampMillis,now)}catch(e:Exception){fail(ENLIL,now,e)}}
 private fun lastModified(r:HttpResult)=r.headers.entries.firstOrNull{it.key.equals("Last-Modified",true)}?.value?.firstOrNull()?.let{runCatching{ZonedDateTime.parse(it,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()}.getOrNull()}
 private suspend fun attempt(k:DataSourceKey,n:Long){val p=statusDao.get(k);statusDao.upsert(p?.copy(lastAttemptTimestampMillis=n)?:DataSourceStatusEntity(k,null,null,null,n,null,null))}
 private suspend fun success(k:DataSourceKey,o:Long,n:Long)=statusDao.upsert(DataSourceStatusEntity(k,o,n,n,n,null,null))
 private suspend fun fail(k:DataSourceKey,n:Long,e:Exception){val p=statusDao.get(k);statusDao.upsert(DataSourceStatusEntity(k,p?.observationTimestampMillis,p?.fetchedTimestampMillis,p?.lastSuccessTimestampMillis,n,n,e.message?:"Refresh failed"))}
 private fun <T> state(d:T?,has:Boolean,s:DataSourceStatusEntity?,k:DataSourceKey):RepositoryState<T>{val er=s?.lastErrorTimestampMillis;val ok=s?.lastSuccessTimestampMillis;if(er!=null&&(ok==null||er>=ok))return RepositoryState.Failure(k,s.lastErrorMessage?:"Refresh failed",d.takeIf{has},DataFreshness.Cached.takeIf{has});if(!has&&ok==null)return RepositoryState.Loading;val f=when(val r=FreshnessEvaluator.evaluate(nowMillis(),s?.observationTimestampMillis,s?.fetchedTimestampMillis,ok!=null,has||ok!=null,FreshnessPolicies.all.getValue(FreshnessSource.IMAGERY_METADATA))){is FreshnessResult.Available->r.freshness;FreshnessResult.Unavailable->DataFreshness.Cached};@Suppress("UNCHECKED_CAST") return if(has)RepositoryState.Available(d as T,k,f)else RepositoryState.Empty(k,f)}
 private fun sourceFor(t:SolarImageType)=when(t){SolarImageType.Magnetogram->HMI;SolarImageType.LascoC2->LASCO_C2;SolarImageType.LascoC3->LASCO_C3;SolarImageType.Aia304->DataSourceKey("helioviewer-aia304")}
 companion object{private const val HMI_LOOKBACK_MILLIS=24*60*60*1000L;private const val ACTIVE_REGION_LOOKBACK_MILLIS=24*60*60*1000L;val HMI=DataSourceKey("worker-hmi");val LASCO_C2=DataSourceKey("worker-lasco-c2");val LASCO_C3=DataSourceKey("worker-lasco-c3");val HEK=DataSourceKey("worker-hek");val ENLIL=DataSourceKey("worker-enlil")}
}
