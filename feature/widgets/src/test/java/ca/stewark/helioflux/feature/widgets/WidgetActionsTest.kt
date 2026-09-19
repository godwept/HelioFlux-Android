package ca.stewark.helioflux.feature.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetActionsTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun routesMatchAppContract() {
        val space=WidgetActions.intent(context,"space-weather",null)
        val aurora=WidgetActions.intent(context,"space-weather","aurora-globe")
        val sun=WidgetActions.intent(context,"home","sun-hero")
        assertEquals("space-weather",space.getStringExtra("ca.stewark.helioflux.extra.WIDGET_DESTINATION"))
        assertEquals("aurora-globe",aurora.getStringExtra("ca.stewark.helioflux.extra.WIDGET_FOCUS"))
        assertEquals("sun-hero",sun.getStringExtra("ca.stewark.helioflux.extra.WIDGET_FOCUS"))
    }
}
