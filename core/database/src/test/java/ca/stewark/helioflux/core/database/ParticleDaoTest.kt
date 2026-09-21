package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParticleDaoTest {
 @Test fun particleSeriesPersistNullableChannelsAndQuerySeventyTwoHours() = runBlocking {
  val db=inMemoryDatabase(); try {
   val hour=3_600_000L; val now=100*hour
   db.xrayFluxDao().upsertAll(listOf(XrayFluxEntity(now-73*hour,1.0,null,null,null),XrayFluxEntity(now-72*hour,null,2.0,null,4.0),XrayFluxEntity(now,5.0,6.0,7.0,null)))
   val xr=db.xrayFluxDao().getBetween(now-72*hour,now); assertEquals(2,xr.size); assertEquals(null,xr.first().goes18Short); assertEquals(null,xr.last().goes19Long)
   db.aceEpamDao().upsertAll(listOf(AceEpamEntity(now-72*hour,null,2.0,3.0,null,5.0,7.0,8.0),AceEpamEntity(now,1.0,null,null,4.0,null,null,9.0)))
   val ep=db.aceEpamDao().getBetween(now-72*hour,now); assertEquals(2,ep.size); assertEquals(null,ep.first().electronLow); assertEquals(7.0,ep.first().protonFp6!!,0.0); assertEquals(9.0,ep.last().protonP7!!,0.0)
   db.xrayFluxDao().deleteBefore(now-72*hour); db.aceEpamDao().deleteBefore(now-72*hour)
  } finally { db.close() }
 }
}
