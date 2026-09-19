package ca.stewark.helioflux.feature.widgets
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import ca.stewark.helioflux.core.data.repository.SolarHeroRepository
import java.net.URL
import java.util.concurrent.TimeUnit
interface SunWidgetDependenciesProvider { fun solarHeroRepository():SolarHeroRepository }
class SunWidgetRefreshWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result=try{val p=applicationContext as? SunWidgetDependenciesProvider?:return Result.failure();val image=p.solarHeroRepository().refreshLatest();if(image!=null){val target=SunWidgetFrameStore(applicationContext).file();URL(image.url).openStream().use{input->target.outputStream().use{input.copyTo(it)}}};SunHeroWidget().updateAll(applicationContext);Result.success()}catch(_:Exception){Result.retry()}
}
class SunWidgetRefreshScheduler(private val context:Context){
 fun schedule()=WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK,ExistingPeriodicWorkPolicy.KEEP,request())
 companion object{const val UNIQUE_WORK="sun-widget-refresh";fun request():PeriodicWorkRequest=PeriodicWorkRequestBuilder<SunWidgetRefreshWorker>(30,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()}
}
