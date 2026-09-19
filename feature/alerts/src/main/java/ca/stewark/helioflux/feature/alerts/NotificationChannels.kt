package ca.stewark.helioflux.feature.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val SPACE_WEATHER_ALERTS_ID = "space_weather_alerts"
    const val SPACE_WEATHER_ALERTS_NAME = "Space weather alerts"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SPACE_WEATHER_ALERTS_ID,
                SPACE_WEATHER_ALERTS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }
}
