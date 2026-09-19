package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
class ImageryTransformState{var scale by mutableFloatStateOf(1f);private set;var offset by mutableStateOf(Offset.Zero);private set;fun transform(zoomChange:Float,panChange:Offset){scale=(scale*zoomChange).coerceIn(1f,5f);offset=if(scale==1f)Offset.Zero else offset+panChange};fun reset(){scale=1f;offset=Offset.Zero}}
@Composable fun FullscreenImageryViewer(title:String,imageUrl:String?,onDismiss:()->Unit,overlay:(@Composable BoxScope.()->Unit)?=null){val transform=remember(imageUrl){ImageryTransformState()};val gesture=rememberTransformableState{zoom,pan,_->transform.transform(zoom,pan)};AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("Close")}},title={Text(title)},text={Box(Modifier.fillMaxWidth().aspectRatio(1f).transformable(gesture)){if(imageUrl!=null)AsyncImage(imageUrl,title,Modifier.fillMaxSize().graphicsLayer{scaleX=transform.scale;scaleY=transform.scale;translationX=transform.offset.x;translationY=transform.offset.y},contentScale=ContentScale.Fit)else Text("Image unavailable");overlay?.invoke(this)}})}
