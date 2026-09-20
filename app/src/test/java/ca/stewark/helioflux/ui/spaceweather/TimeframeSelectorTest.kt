package ca.stewark.helioflux.ui.spaceweather
import org.junit.Assert.assertEquals
import org.junit.Test
class TimeframeSelectorTest{
 @Test fun selectorHasFourStableChoicesAndLabels(){assertEquals(listOf(Timeframe.OneHour,Timeframe.ThreeHours,Timeframe.TwelveHours,Timeframe.TwoDays),timeframeOptions);assertEquals(listOf("1h","3h","12h","2d"),timeframeOptions.map{it.label})}
 @Test fun defaultSelectionContractIsTwelveHours(){assertEquals(Timeframe.TwelveHours,defaultTimeframe)}
}
