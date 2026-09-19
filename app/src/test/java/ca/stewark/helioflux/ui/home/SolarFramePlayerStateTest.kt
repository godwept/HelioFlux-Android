package ca.stewark.helioflux.ui.home
import org.junit.Assert.assertEquals
import org.junit.Test
class SolarFramePlayerStateTest{@Test fun advanceWrapsAndResetReturnsFirstFrame(){val s=SolarFramePlayerState({3});s.advance();assertEquals(1,s.index);s.advance();s.advance();assertEquals(0,s.index);s.advance();s.reset();assertEquals(0,s.index)}@Test fun emptyFramesStayAtZero(){val s=SolarFramePlayerState({0});s.advance();assertEquals(0,s.index)}}
