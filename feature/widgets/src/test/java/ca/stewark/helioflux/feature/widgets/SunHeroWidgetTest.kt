package ca.stewark.helioflux.feature.widgets
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class) class SunHeroWidgetTest{
 @Test fun missingFrameShowsFallbackState(){val c=ApplicationProvider.getApplicationContext<Context>();SunWidgetFrameStore(c).file().delete();assertNull(SunWidgetFrameStore(c).read())}
 @Test fun latestCachedFrameIsReadable(){val c=ApplicationProvider.getApplicationContext<Context>();val f=SunWidgetFrameStore(c).file();f.outputStream().use{Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG,100,it)};assertNotNull(SunWidgetFrameStore(c).read())}
}
