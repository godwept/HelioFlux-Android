package ca.stewark.helioflux.feature.widgets

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.stewark.helioflux.core.model.DataFreshness

class SpaceWeatherStatusWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(180.dp, 110.dp),
            androidx.compose.ui.unit.DpSize(280.dp, 110.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SpaceWeatherStatusContent(SpaceWeatherWidgetDefaults.model) }
    }
}

object SpaceWeatherWidgetDefaults {
    var model = SpaceWeatherWidgetModel("Kp --", "Bz --", "-- km/s", "M -- · X --", DataFreshness.Cached)
}

@Composable
internal fun SpaceWeatherStatusContent(model: SpaceWeatherWidgetModel) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row {
            Text("SPACE WEATHER", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text("  " + freshnessLabel(model.freshness), style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.Gray)))
        }
        Text(model.kp, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Row {
            Text(model.bz, modifier = GlanceModifier.padding(end = 12.dp))
            Text(model.speed)
        }
        Text(model.flareStatus, style = TextStyle(fontSize = 12.sp))
    }
}

internal fun freshnessLabel(freshness: DataFreshness) = when (freshness) {
    DataFreshness.Fresh -> "LIVE"
    DataFreshness.Delayed -> "DELAYED"
    DataFreshness.Cached -> "CACHED"
}
