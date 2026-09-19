package ca.stewark.helioflux.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ca.stewark.helioflux.core.model.DataFreshness
import kotlinx.coroutines.delay

const val DefaultFreshnessLabelDurationMillis = 3_000L

@Composable
fun FreshnessIndicator(
    freshness: DataFreshness,
    modifier: Modifier = Modifier,
    labelDurationMillis: Long = DefaultFreshnessLabelDurationMillis,
) {
    var showLabel by remember { mutableStateOf(true) }
    var revealToken by remember { mutableIntStateOf(0) }
    val label = freshness.name
    val dotColor = when (freshness) {
        DataFreshness.Fresh -> Color(0xFF4CAF50)
        DataFreshness.Delayed -> Color(0xFFFFB300)
        DataFreshness.Cached -> Color(0xFF78909C)
    }

    LaunchedEffect(freshness, revealToken, labelDurationMillis) {
        showLabel = true
        delay(labelDurationMillis.coerceAtLeast(0L))
        showLabel = false
    }

    Row(
        modifier = modifier
            .semantics { contentDescription = "Data freshness: $label" }
            .clickable { revealToken++ }
            .testTag("freshness-indicator"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier
                .size(8.dp)
                .testTag("freshness-dot"),
        ) {
            drawCircle(dotColor)
        }
        AnimatedVisibility(visible = showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("freshness-label"),
            )
        }
    }
}
