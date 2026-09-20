package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.*
import org.junit.Test
class KpChartTest{
 @Test fun thresholdBoundariesUseSharedMapper(){assertEquals(KpStatus.Unsettled,kpStatus(3.0));assertEquals(KpStatus.Minor,kpStatus(5.0));assertEquals(KpStatus.Moderate,kpStatus(6.0));assertEquals(KpStatus.Strong,kpStatus(7.0));assertEquals(KpStatus.Severe,kpStatus(9.0))}
 @Test fun currentStatusUsesLatestNonNullKp(){val now=10000L;val p=kpPresentation(listOf(KpSample(now-2,4.0),KpSample(now-1,6.0)),Timeframe.OneHour,now);assertEquals(KpStatus.Moderate,p.currentStatus)}
 @Test fun shortTimeframesStillShowLatestKpBucket(){val now=10_000_000L;val latest=KpSample(now-(2*60*60*1000L),4.0);val p=kpPresentation(listOf(latest),Timeframe.OneHour,now);assertEquals(1,p.values.size);assertEquals(4.0,p.values.single().second,0.0)}
 @Test fun presentationCopyIsStable(){assertEquals("Geomagnetic Activity",kpChartMeta.context);assertEquals("Planetary Kp Index (3-hour)",kpChartMeta.title);assertEquals("No current Kp",kpStatusLabel(null))}
}
