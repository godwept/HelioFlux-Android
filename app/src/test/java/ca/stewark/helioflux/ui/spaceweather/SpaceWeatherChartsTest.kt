package ca.stewark.helioflux.ui.spaceweather
import ca.stewark.helioflux.core.data.repository.GoesMagnetometerSeries
import ca.stewark.helioflux.core.model.*
import ca.stewark.helioflux.ui.components.ChartReferenceLine
import org.junit.Assert.*
import org.junit.Test
class SpaceWeatherChartsTest{
 private val now=10_000_000L
 @Test fun bzBtUsesFilteredDataAndZeroReference(){val s=listOf(SolarWindMag(now-4*60*60*1000,null,null,-9.0,10.0),SolarWindMag(now-1000,null,null,-2.0,4.0));val out=bzBtSeries(s,Timeframe.OneHour,now);assertEquals(listOf(-2.0),out[0].points.map{it.y});assertEquals(0.0,ChartReferenceLine(0.0).y,0.0)}
 @Test fun plasmaFieldsKeepMissingValuesNull(){val s=listOf(SolarWindPlasma(now-1,5.0,null,90000.0));assertNull(plasmaSeries(s,Timeframe.OneHour,now){it.speed}[0].points.single().y);assertEquals(5.0,plasmaSeries(s,Timeframe.OneHour,now){it.density}[0].points.single().y!!,0.0);assertEquals(90000.0,plasmaSeries(s,Timeframe.OneHour,now){it.temperature}[0].points.single().y!!,0.0)}
 @Test fun goesKeepsDynamicLabelsAndNullGaps(){val g=GoesMagnetometerSeries(listOf(GoesMagSample(now-2,1.0,2.0),GoesMagSample(now-1,null,3.0)),"P","S");val out=goesSeries(g,Timeframe.OneHour,now);assertEquals("P",g.primaryLabel);assertEquals("S",g.secondaryLabel);assertNull(out[0].points.last().y)}
 @Test fun hemisphericPowerHasNorthAndSouthSeries(){val out=hemisphericPowerSeries(listOf(HemisphericPowerSample(now-1,40.0,30.0)),Timeframe.OneHour,now);assertEquals(40.0,out[0].points.single().y!!,0.0);assertEquals(30.0,out[1].points.single().y!!,0.0)}
}
