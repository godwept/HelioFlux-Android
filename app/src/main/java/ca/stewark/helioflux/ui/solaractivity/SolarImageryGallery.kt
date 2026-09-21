package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.ActiveRegion
import ca.stewark.helioflux.core.model.EnlilFrame
import ca.stewark.helioflux.core.model.SolarImage
import ca.stewark.helioflux.ui.theme.SolarOrange
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface

enum class SolarGalleryItem {
    Hmi,
    LascoC2,
    LascoC3,
    Enlil,
}

internal val DefaultSolarGalleryItem = SolarGalleryItem.Hmi

val SolarGalleryItem.selectorLabel: String
    get() =
        when (this) {
            SolarGalleryItem.Hmi -> "HMI"
            SolarGalleryItem.LascoC2 -> "C2"
            SolarGalleryItem.LascoC3 -> "C3"
            SolarGalleryItem.Enlil -> "ENLIL"
        }

private val SolarGalleryItem.stageTag: String
    get() =
        when (this) {
            SolarGalleryItem.Hmi -> "solar-imagery-stage-hmi"
            SolarGalleryItem.LascoC2 -> "solar-imagery-stage-c2"
            SolarGalleryItem.LascoC3 -> "solar-imagery-stage-c3"
            SolarGalleryItem.Enlil -> "solar-imagery-stage-enlil"
        }

@Composable
fun SolarImageryGallery(
    selected: SolarGalleryItem,
    onSelected: (SolarGalleryItem) -> Unit,
    magnetogram: RepositoryState<SolarImage>,
    lascoC2: RepositoryState<SolarImage>,
    lascoC3: RepositoryState<SolarImage>,
    regions: List<ActiveRegion>,
    enlil: RepositoryState<List<EnlilFrame>>,
    onOpen: (SolarGalleryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().testTag("solar-imagery-selector"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SolarGalleryItem.entries.forEach { item ->
                val isSelected = item == selected
                val shape = RoundedCornerShape(50)
                Surface(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable { onSelected(item) }
                            .testTag("solar-imagery-" + item.selectorLabel.lowercase()),
                    color = if (isSelected) SolarOrange.copy(alpha = 0.16f) else SpaceSurface,
                    shape = shape,
                    border =
                        BorderStroke(
                            1.dp,
                            if (isSelected) SolarOrange else SpaceMuted.copy(alpha = 0.30f),
                        ),
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.selectorLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) SolarOrange else SpaceMuted,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        when (selected) {
            SolarGalleryItem.Hmi ->
                MagnetogramCard(
                    state = magnetogram,
                    regions = regions,
                    onClick = { onOpen(selected) },
                    modifier = Modifier.fillMaxWidth().testTag(selected.stageTag),
                )
            SolarGalleryItem.LascoC2 ->
                SolarImageryCard(
                    "LASCO C2",
                    "SOHO / LASCO",
                    lascoC2,
                    { onOpen(selected) },
                    Modifier.fillMaxWidth().testTag(selected.stageTag),
                )
            SolarGalleryItem.LascoC3 ->
                SolarImageryCard(
                    "LASCO C3",
                    "SOHO / LASCO",
                    lascoC3,
                    { onOpen(selected) },
                    Modifier.fillMaxWidth().testTag(selected.stageTag),
                )
            SolarGalleryItem.Enlil ->
                EnlilCard(
                    state = enlil,
                    visible = true,
                    onClick = { onOpen(selected) },
                    modifier = Modifier.fillMaxWidth().testTag(selected.stageTag),
                )
        }
    }
}
