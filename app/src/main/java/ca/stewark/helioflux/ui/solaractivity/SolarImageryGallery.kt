package ca.stewark.helioflux.ui.solaractivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
enum class SolarGalleryItem{Hmi,LascoC2,LascoC3,Enlil}
@Composable fun SolarImageryGallery(expanded:Boolean,magnetogram:RepositoryState<SolarImage>,lascoC2:RepositoryState<SolarImage>,lascoC3:RepositoryState<SolarImage>,regions:List<ActiveRegion>,enlil:RepositoryState<List<EnlilFrame>>,onOpen:(SolarGalleryItem)->Unit){@Composable fun Item(item:SolarGalleryItem,modifier:Modifier=Modifier){Box(modifier){when(item){SolarGalleryItem.Hmi->MagnetogramCard(magnetogram,regions){onOpen(item)};SolarGalleryItem.LascoC2->SolarImageryCard("LASCO C2","SOHO / LASCO",lascoC2,{onOpen(item)});SolarGalleryItem.LascoC3->SolarImageryCard("LASCO C3","SOHO / LASCO",lascoC3,{onOpen(item)});SolarGalleryItem.Enlil->EnlilCard(enlil,true,{onOpen(item)})}}};val galleryItems=SolarGalleryItem.entries;if(!expanded){LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items(galleryItems){Item(it,Modifier.width(300.dp))}}}else{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){galleryItems.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{Item(it,Modifier.weight(1f))};if(row.size==1)Spacer(Modifier.weight(1f))}}}}}
