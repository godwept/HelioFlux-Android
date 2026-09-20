package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
 val player=remember(playable){EnlilPlayerState({playable.size})}
 var index by remember(playable){mutableIntStateOf(0)}
 LaunchedEffect(player,visible,preloaded){if(visible&&playable.size>1&&preloaded.isNotEmpty())player.play({isActive&&visible}){index=it}}
 Card(modifier.clickable(enabled=frames.isNotEmpty(),onClick=onClick)){Column{
  Box(Modifier.fillMaxWidth().aspectRatio(1f)){
   if(playable.isNotEmpty())Crossfade(targetState=index.coerceAtMost(playable.lastIndex),animationSpec=tween(200),label="enlil-frame"){i->
    AsyncImage(playable[i],"WSA-Enlil frame",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
   }
   if(frames.isEmpty())Text("ENLIL frames unavailable",Modifier.padding(16.dp))
  }
  Text("WSA-Enlil",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(12.dp))
 }}
}
