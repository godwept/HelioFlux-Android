package ca.stewark.helioflux.feature.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity

internal object WidgetActions {
    private const val MAIN_ACTIVITY = "ca.stewark.helioflux.MainActivity"
    private const val EXTRA_DESTINATION = "ca.stewark.helioflux.extra.WIDGET_DESTINATION"
    private const val EXTRA_FOCUS = "ca.stewark.helioflux.extra.WIDGET_FOCUS"

    fun spaceWeather(context: Context): Action = actionStartActivity(intent(context,"space-weather",null))
    fun aurora(context: Context): Action = actionStartActivity(intent(context,"space-weather","aurora-globe"))
    fun sun(context: Context): Action = actionStartActivity(intent(context,"home","sun-hero"))

    internal fun intent(context: Context,destination:String,focus:String?):Intent =
        Intent().setClassName(context.packageName,MAIN_ACTIVITY)
            .putExtra(EXTRA_DESTINATION,destination)
            .apply { focus?.let { putExtra(EXTRA_FOCUS,it) } }
}
