package ca.stewark.helioflux.ui.solaractivity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.FlareProbabilities
import ca.stewark.helioflux.ui.theme.SpaceMuted
import ca.stewark.helioflux.ui.theme.SpaceSurface

@Composable
fun FlareProbabilityBadges(
    state: RepositoryState<FlareProbabilities>,
    modifier: Modifier = Modifier,
) {
    val presentation = flareProbabilityPresentation(state)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (presentation.metrics.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().testTag("flare-probability-strip"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                presentation.metrics.forEach { metric ->
                    val shape = RoundedCornerShape(50)
                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .shadow(
                                    elevation = 5.dp,
                                    shape = shape,
                                    ambientColor = metric.accent.copy(alpha = 0.28f),
                                    spotColor = metric.accent.copy(alpha = 0.38f),
                                )
                                .testTag("flare-probability-" + metric.label.lowercase()),
                        color = SpaceSurface,
                        shape = shape,
                        border = BorderStroke(1.dp, metric.accent.copy(alpha = 0.52f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                metric.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = metric.accent,
                            )
                            Text(
                                metric.value,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        presentation.message?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = SpaceMuted)
        }
    }
}
