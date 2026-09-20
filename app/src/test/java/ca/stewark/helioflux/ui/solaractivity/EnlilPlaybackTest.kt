package ca.stewark.helioflux.ui.solaractivity

import org.junit.Assert.assertEquals
import org.junit.Test

class EnlilPlaybackTest {
 @Test fun playbackUsesPreloadedFramesAndKeepsPosterWhileLoading(){
  val urls=listOf("one","two","three")
  assertEquals(listOf("one"),selectPlayableEnlilFrames(urls,emptySet()))
  assertEquals(listOf("one","three"),selectPlayableEnlilFrames(urls,setOf("one","three")))
 }
 @Test fun blendProgressRunsContinuouslyAcrossFrameInterval(){
  assertEquals(0f,enlilBlendProgress(0L,200L),0f)
  assertEquals(0.5f,enlilBlendProgress(100L,200L),0f)
  assertEquals(1f,enlilBlendProgress(200L,200L),0f)
 }
}
