package ca.stewark.helioflux.feature.widgets

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.io.File

class SunHeroWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bitmap=SunWidgetFrameStore(context).read()
        provideContent { SunHeroContent(bitmap?.let(::ImageProvider),WidgetActions.sun(context)) }
    }
}
internal class SunWidgetFrameStore(private val context:Context) {
    private val file get()=File(context.filesDir,"sun-widget-aia304.png")
    fun read()=if(file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    fun file()=file
}
@Composable internal fun SunHeroContent(image:ImageProvider?,action:androidx.glance.action.Action?=null) {
    Column(GlanceModifier.fillMaxSize().padding(12.dp).let{if(action!=null)it.clickable(action) else it},horizontalAlignment=Alignment.Horizontal.CenterHorizontally,verticalAlignment=Alignment.Vertical.CenterVertically) {
        if(image!=null) Image(image,"Latest AIA 304 solar image",GlanceModifier.size(140.dp))
        else { Text("SUN",style=TextStyle(fontSize=20.sp));Text("Solar image unavailable",style=TextStyle(fontSize=12.sp)) }
    }
}
