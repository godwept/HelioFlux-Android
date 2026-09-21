package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.data.freshness.*
import ca.stewark.helioflux.core.data.network.*
import ca.stewark.helioflux.core.data.parser.*
import ca.stewark.helioflux.core.database.dao.*
import ca.stewark.helioflux.core.database.RetentionPolicy
import ca.stewark.helioflux.core.database.entity.DataSourceStatusEntity
import ca.stewark.helioflux.core.database.toDomain
import ca.stewark.helioflux.core.database.toEntity
import ca.stewark.helioflux.core.model.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class SolarActivityRepository(private val xrayDao:XrayFluxDao,private val flareDao:FlareEventDao,private val cmeDao:CmeEventDao,private val aceEpamDao:AceEpamDao,private val statusDao:DataSourceStatusDao,private val transport:HttpTransport,private val nowMillis:()->Long=System::currentTimeMillis){
 private val probabilities=MutableStateFlow<FlareProbabilities?>(null)
 fun flareProbabilities():Flow<RepositoryState<FlareProbabilities>> = combine(probabilities,statusDao.observe(FLARE_PROBABILITIES)){d,s->state(d,d!=null,s,FLARE_PROBABILITIES,FreshnessSource.REALTIME_NOAA)}
 fun xray(startMillis:Long,endMillis:Long)=combine(xrayDao.observeBetween(startMillis,endMillis),statusDao.observe(XRAY)){r,s->state(r.map{it.toDomain()},r.isNotEmpty(),s,XRAY,FreshnessSource.REALTIME_NOAA)}
 fun flares()=combine(flareDao.observeNewestFirst(),statusDao.observe(DONKI_FLARES)){r,s->state(r.map{it.toDomain()},r.isNotEmpty(),s,DONKI_FLARES,FreshnessSource.DONKI_EVENTS)}
 fun cmes()=combine(cmeDao.observeNewestFirst(),statusDao.observe(DONKI_CMES)){r,s->state(r.map{it.toDomain()},r.isNotEmpty(),s,DONKI_CMES,FreshnessSource.DONKI_EVENTS)}
 fun aceEpam(startMillis:Long,endMillis:Long)=combine(aceEpamDao.observeBetween(startMillis,endMillis),statusDao.observe(ACE_EPAM)){r,s->state(r.map{it.toDomain()},r.isNotEmpty(),s,ACE_EPAM,FreshnessSource.REALTIME_NOAA)}
 suspend fun refreshFlareProbabilities(){val n=nowMillis();attempt(FLARE_PROBABILITIES,n);try{val p=FlareProbabilityParser.parse(transport.get(HelioFluxEndpoints.flareProbabilities).body());require(p.c>0||p.m>0||p.x>0){"Flare probability response contained no valid probabilities"};probabilities.value=p;success(FLARE_PROBABILITIES,n,n)}catch(e:Exception){fail(FLARE_PROBABILITIES,n,e)}}
 suspend fun refreshXray(){val n=nowMillis();attempt(XRAY,n);try{val all=XrayFluxParser.parse(transport.get(HelioFluxEndpoints.goesPrimaryXray).body())+XrayFluxParser.parse(transport.get(HelioFluxEndpoints.goesSecondaryXray).body());val p=all.groupBy{it.timestampMillis}.map{(t,v)->XrayFluxSample(t,v.firstNotNullOfOrNull{it.goes18Short},v.firstNotNullOfOrNull{it.goes18Long},v.firstNotNullOfOrNull{it.goes19Short},v.firstNotNullOfOrNull{it.goes19Long})}.filter{it.timestampMillis>=n-WINDOW}.sortedBy{it.timestampMillis};require(p.isNotEmpty()){"GOES X-ray response contained no valid rows"};xrayDao.upsertAll(p.map(XrayFluxSample::toEntity));xrayDao.deleteBefore(RetentionPolicy.solarActivityCutoff(n));success(XRAY,p.maxOf{it.timestampMillis},n)}catch(e:Exception){fail(XRAY,n,e)}}
 suspend fun refreshFlares()=donki(DONKI_FLARES,"FLR",parse=DonkiFlareParser::parse,persist={flareDao.upsertAll(it.map(FlareEvent::toEntity));flareDao.deleteBefore(RetentionPolicy.eventCutoff(nowMillis()))},obs={it.maxOfOrNull(FlareEvent::timestampMillis)})
 suspend fun refreshCmes()=donki(DONKI_CMES,"CMEAnalysis","&mostAccurateOnly=true",DonkiCmeParser::parse,{cmeDao.upsertAll(it.map(CmeEvent::toEntity));cmeDao.deleteBefore(RetentionPolicy.eventCutoff(nowMillis()))},{it.maxOfOrNull(CmeEvent::timestampMillis)})
 suspend fun refreshAceEpam(){val n=nowMillis();attempt(ACE_EPAM,n);try{val url=HelioFluxEndpoints.aceEpam(n-WINDOW,n);val p=AceEpamParser.last72Hours(AceEpamParser.parse(transport.get(url).body()),n);require(p.isNotEmpty()){"ACE EPAM response contained no valid rows"};aceEpamDao.upsertAll(p.map(AceEpamSample::toEntity));aceEpamDao.deleteBefore(RetentionPolicy.solarActivityCutoff(n));success(ACE_EPAM,p.maxOf{it.timestampMillis},n)}catch(e:Exception){fail(ACE_EPAM,n,e)}}
 private suspend fun <T> donki(source:DataSourceKey,path:String,extra:String="",parse:(String)->List<T>,persist:suspend(List<T>)->Unit,obs:(List<T>)->Long?){val n=nowMillis();attempt(source,n);try{val url=HelioFluxEndpoints.donki+"/"+path+"?startDate="+date(n-WINDOW)+"&endDate="+date(n)+extra;val p=parse(transport.get(url).body());persist(p);success(source,obs(p)?:n,n)}catch(e:Exception){fail(source,n,e)}}
 private suspend fun attempt(s:DataSourceKey,n:Long){val p=statusDao.get(s);statusDao.upsert(p?.copy(lastAttemptTimestampMillis=n)?:DataSourceStatusEntity(s,null,null,null,n,null,null))}
 private suspend fun success(s:DataSourceKey,o:Long,n:Long){statusDao.upsert(DataSourceStatusEntity(s,o,n,n,n,null,null))}
 private suspend fun fail(s:DataSourceKey,n:Long,e:Exception){val p=statusDao.get(s);statusDao.upsert(DataSourceStatusEntity(s,p?.observationTimestampMillis,p?.fetchedTimestampMillis,p?.lastSuccessTimestampMillis,n,n,e.message?:"Refresh failed"))}
 private fun <T> state(d:T?,has:Boolean,s:DataSourceStatusEntity?,key:DataSourceKey,fs:FreshnessSource):RepositoryState<T>{val er=s?.lastErrorTimestampMillis;val ok=s?.lastSuccessTimestampMillis;if(er!=null&&(ok==null||er>=ok))return RepositoryState.Failure(key,s.lastErrorMessage?:"Refresh failed",d.takeIf{has},DataFreshness.Cached.takeIf{has});if(!has&&ok==null)return RepositoryState.Loading;val f=when(val r=FreshnessEvaluator.evaluate(nowMillis(),s?.observationTimestampMillis,s?.fetchedTimestampMillis,ok!=null,has||ok!=null,FreshnessPolicies.all.getValue(fs))){is FreshnessResult.Available->r.freshness;FreshnessResult.Unavailable->DataFreshness.Cached};@Suppress("UNCHECKED_CAST") return if(has)RepositoryState.Available(d as T,key,f)else RepositoryState.Empty(key,f)}
 private fun HttpResult.body():String{require(status in 200..299){"HTTP "+status};return body}
 private fun date(m:Long)=DATE.format(Instant.ofEpochMilli(m))
 companion object{private const val WINDOW=72L*60*60*1000;private val DATE=DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);val FLARE_PROBABILITIES=DataSourceKey("noaa-flare-probabilities");val XRAY=DataSourceKey("noaa-goes-xray");val DONKI_FLARES=DataSourceKey("donki-flares");val DONKI_CMES=DataSourceKey("donki-cmes");val ACE_EPAM=DataSourceKey("noaa-ace-epam")}
}
