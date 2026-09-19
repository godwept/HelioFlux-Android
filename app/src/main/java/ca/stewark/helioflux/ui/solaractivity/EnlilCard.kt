package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.EnlilFrame
import coil3.compose.AsyncImage
import kotlinx.coroutines.isActive
@Composable fun EnlilCard(state:RepositoryState<List<EnlilFrame>>,visible:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){val frames=when(state){is RepositoryState.Available->state.data;is RepositoryState.Failure->state.retainedData.orEmpty();else->emptyList()};val player=remember(frames){EnlilPlayerState({frames.size})};var index by remember(frames){mutableIntStateOf(0)};LaunchedEffect(player,visible){if(visible&&frames.isNotEmpty())player.play({isActive&&visible}){index=it}};Card(modifier.clickable(enabled=frames.isNotEmpty(),onClick=onClick)){Column{Box(Modifier.fillMaxWidth().aspectRatio(1f)){frames.getOrNull(index)?.let{AsyncImage(it.url,"WSA-Enlil frame",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)};if(frames.isEmpty())Text("ENLIL frames unavailable",Modifier.padding(16.dp))};Text("WSA-Enlil",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(12.dp))}}}
