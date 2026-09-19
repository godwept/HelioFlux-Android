package ca.stewark.helioflux.ui.spaceweather
import org.junit.Assert.assertEquals
import org.junit.Test
class SpaceWeatherScreenTest{@Test fun compactStacksAndExpandedUsesTwoColumns(){assertEquals(1,spaceWeatherColumnCount(false));assertEquals(2,spaceWeatherColumnCount(true))}}
