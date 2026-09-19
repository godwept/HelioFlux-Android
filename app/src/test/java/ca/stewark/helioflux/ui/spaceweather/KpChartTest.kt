package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.*
import org.junit.Test
class KpChartTest{
 @Test fun thresholdBoundariesUseSharedMapper(){assertEquals(KpStatus.Unsettled,kpStatus(3.0));assertEquals(KpStatus.Minor,kpStatus(5.0));assertEquals(KpStatus.Moderate,kpStatus(6.0));assertEquals(KpStatus.Strong,kpStatus(7.0));assertEquals(KpStatus.Severe,kpStatus(9.0))}
 @Test fun currentStatusUsesLatestNonNullKp(){val now=10000L;val p=kpPresentation(listOf(KpSample(now-2,4.0),KpSample(now-1,6.0)),Timeframe.OneHour,now);assertEquals(KpStatus.Moderate,p.currentStatus)}
}
