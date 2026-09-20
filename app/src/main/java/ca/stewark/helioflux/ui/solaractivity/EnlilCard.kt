package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.EnlilFrame
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

internal fun enlilBlendProgress(elapsedMillis:Long,cadenceMillis:Long):Float =
 if(cadenceMillis<=0L)1f else (elapsedMillis.toFloat()/cadenceMillis).coerceIn(0f,1f)

internal fun selectPlayableEnlilFrames(urls:List<String>,preloadedUrls:Set<String>):List<String>{
 val loaded=urls.filter{it in preloadedUrls}
 return loaded.ifEmpty{urls.take(1)}
}
private suspend fun preloadEnlilFrames(context:android.content.Context,urls:List<String>):Set<String> = coroutineScope {
 val loader=SingletonImageLoader.get(context)
 urls.map{url->async{url.takeIf{loader.execute(ImageRequest.Builder(context).data(url).build()) is SuccessResult}}}.awaitAll().filterNotNull().toSet()
}
@Composable fun EnlilCard(state:RepositoryState<List<EnlilFrame>>,visible:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){
 val frames=when(state){is RepositoryState.Available->state.data;is RepositoryState.Failure->state.retainedData.orEmpty();else->emptyList()}
 val urls=frames.map{it.url}
 val context=LocalContext.current
 var preloaded by remember(urls){mutableStateOf<Set<String>>(emptySet())}
 LaunchedEffect(urls){preloaded=if(urls.size>1)preloadEnlilFrames(context,urls) else urls.toSet()}
 val playable=selectPlayableEnlilFrames(urls,preloaded)
 var index by remember(playable){mutableIntStateOf(0)}
 var blend by remember(playable){mutableFloatStateOf(0f)}
 LaunchedEffect(playable,visible,preloaded){
  if(visible&&playable.size>1&&preloaded.isNotEmpty()){
   while(isActive&&visible){
    val started=withFrameNanos{it}
    var elapsed=0L
    while(isActive&&visible&&elapsed<200L){
     withFrameNanos{now->elapsed=(now-started)/1_000_000L}
     blend=enlilBlendProgress(elapsed,200L)
    }
    index=(index+1)%playable.size
    blend=0f
   }
  }
 }
 Card(modifier.clickable(enabled=frames.isNotEmpty(),onClick=onClick)){Column{
  Box(Modifier.fillMaxWidth().aspectRatio(1f)){
   if(playable.isNotEmpty()){
    val current=index.coerceAtMost(playable.lastIndex)
    val next=(current+1)%playable.size
    AsyncImage(playable[current],"WSA-Enlil frame",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    if(playable.size>1)AsyncImage(playable[next],"WSA-Enlil next frame",Modifier.fillMaxSize().graphicsLayer(alpha=blend),contentScale=ContentScale.Fit)
   }
   if(frames.isEmpty())Text("ENLIL frames unavailable",Modifier.padding(16.dp))
  }
  Text("WSA-Enlil",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(12.dp))
 }}
}
