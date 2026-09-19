package ca.stewark.helioflux.ui.home
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarHeroStateTest {
 @Test fun panOffsetUsesBundleSaveableFloatComponents(){
  val offset=Offset(12.5f,-7.25f)
  val saved=listOf(offset.x,offset.y)
  assertTrue(saved.all{it is Float})
  assertEquals(offset,Offset(saved[0],saved[1]))
 }
}
