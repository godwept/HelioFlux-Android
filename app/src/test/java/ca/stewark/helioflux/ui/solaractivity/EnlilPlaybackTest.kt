package ca.stewark.helioflux.ui.solaractivity

import org.junit.Assert.assertEquals
import org.junit.Test

class EnlilPlaybackTest {
 @Test fun playbackUsesOnlyPreloadedFramesButKeepsPosterWhileLoading(){
  val urls=listOf("one","two","three")
  assertEquals(listOf("one"),selectPlayableEnlilFrames(urls,emptySet()))
  assertEquals(listOf("one","three"),selectPlayableEnlilFrames(urls,setOf("one","three")))
 }
}
