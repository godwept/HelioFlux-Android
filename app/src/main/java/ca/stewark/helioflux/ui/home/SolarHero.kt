package ca.stewark.helioflux.ui.home
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.SolarImage
import coil3.compose.AsyncImage
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
@Composable fun SolarHero(state:RepositoryState<List<SolarImage>>,modifier:Modifier=Modifier,playing:Boolean=true){
 val frames=when(state){is RepositoryState.Available->state.data;is RepositoryState.Failure->state.retainedData.orEmpty();else->emptyList()}
 var frameIndex by remember(frames.size){mutableIntStateOf(0)};val player=remember(frames.size){SolarFramePlayerState({frames.size})}
 LaunchedEffect(playing,frames.size){if(playing&&frames.isNotEmpty())player.play({isActive&&playing}){frameIndex=it}}
 var scale by rememberSaveable{mutableFloatStateOf(1f)};var offset by rememberSaveable{mutableStateOf(Offset.Zero)}
 val transform=rememberTransformableState{zoom,pan,_->val next=(scale*zoom).coerceIn(1f,4f);scale=next;offset=if(next==1f)Offset.Zero else offset+pan}
 Card(modifier.testTag("solar-hero")){Box(Modifier.fillMaxWidth().aspectRatio(1f).transformable(transform).pointerInput(Unit){detectTapGestures(onDoubleTap={scale=1f;offset=Offset.Zero})},contentAlignment=Alignment.Center){
  frames.getOrNull(frameIndex.coerceAtMost((frames.size-1).coerceAtLeast(0)))?.let{image->AsyncImage(image.url,"Animated AIA 304 Sun",Modifier.fillMaxSize().graphicsLayer(scaleX=scale,scaleY=scale).offset{IntOffset(offset.x.roundToInt(),offset.y.roundToInt())}.testTag("solar-frame-$frameIndex"))}
  if(frames.isEmpty()&&state is RepositoryState.Loading)CircularProgressIndicator()
  if(state is RepositoryState.Failure)Text("Using cached solar imagery",Modifier.align(Alignment.BottomCenter).padding(12.dp))
 }}
}
