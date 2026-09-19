package ca.stewark.helioflux.feature.alerts

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationChannelsTest {
    @Test fun createsOneIdempotentSpaceWeatherChannel() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        NotificationChannels.create(context)
        NotificationChannels.create(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(NotificationChannels.SPACE_WEATHER_ALERTS_ID)
        assertNotNull(channel)
        assertEquals(NotificationChannels.SPACE_WEATHER_ALERTS_NAME, channel.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        assertEquals(1, manager.notificationChannels.count { it.id == NotificationChannels.SPACE_WEATHER_ALERTS_ID })
    }
}
