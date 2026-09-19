package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.*
import org.junit.Test
class SolarWindMetricsTest{
 @Test fun latestNonEmptyValuesAreSelected(){val v=latestSolarWindMetrics(listOf(SolarWindMag(1,null,null,-2.0,5.0),SolarWindMag(2,null,null,null,6.0),SolarWindMag(3,null,null,-4.0,7.0)),listOf(SolarWindPlasma(1,5.0,400.0,null),SolarWindPlasma(2,null,null,null),SolarWindPlasma(3,8.0,450.0,null)));assertEquals(-4.0,v.bz!!,0.0);assertEquals(450.0,v.speed!!,0.0);assertEquals(8.0,v.density!!,0.0)}
 @Test fun missingValuesStayNullForPlaceholderRendering(){assertEquals(SolarWindMetricValues(null,null,null),latestSolarWindMetrics(emptyList(),emptyList()))}
}
