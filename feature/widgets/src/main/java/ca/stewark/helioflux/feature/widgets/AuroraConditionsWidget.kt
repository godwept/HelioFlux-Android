package ca.stewark.helioflux.feature.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import ca.stewark.helioflux.core.model.DataFreshness

class AuroraConditionsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(180.dp, 100.dp), DpSize(280.dp, 120.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { AuroraConditionsContent(AuroraWidgetDefaults.model) }
    }
}

object AuroraWidgetDefaults {
    var model = AuroraConditionsWidgetModel("Kp --", "OVATION --", null, DataFreshness.Cached)
}

@Composable
internal fun AuroraConditionsContent(model: AuroraConditionsWidgetModel) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row {
            Text("AURORA CONDITIONS", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text("  " + freshnessLabel(model.freshness), style = TextStyle(fontSize = 11.sp))
        }
        Text(model.kp, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Text(model.ovationSummary)
        model.hemisphericPower?.let { Text(it, style = TextStyle(fontSize = 12.sp)) }
    }
}
