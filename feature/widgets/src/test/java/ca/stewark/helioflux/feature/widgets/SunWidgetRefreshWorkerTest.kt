package ca.stewark.helioflux.feature.widgets
import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test
class SunWidgetRefreshWorkerTest{@Test fun refreshIsThirtyMinutesAndNetworkConstrained(){val r=SunWidgetRefreshScheduler.request();assertEquals(30*60*1000L,r.workSpec.intervalDuration);assertEquals(NetworkType.CONNECTED,r.workSpec.constraints.requiredNetworkType)}}
