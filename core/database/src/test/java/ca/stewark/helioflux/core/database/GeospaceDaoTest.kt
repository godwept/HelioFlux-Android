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
class GeospaceDaoTest {
 @Test fun geospaceSeriesUpsertQueryAndRetainNullableChannels() = runBlocking {
  val db=inMemoryDatabase(); try {
   db.kpDao().upsertAll(listOf(KpEntity(300,3.0),KpEntity(100,1.0),KpEntity(200,2.0))); db.kpDao().upsertAll(listOf(KpEntity(200,2.5))); db.kpDao().deleteBefore(150)
   assertEquals(listOf(200L,300L),db.kpDao().getBetween(0,400).map{it.timestampMillis}); assertEquals(2.5,db.kpDao().getBetween(0,400).first().kp!!,0.0)
   db.goesMagDao().upsertAll(listOf(GoesMagEntity(100,null,4.0,"GOES-19","GOES-18")))
   val goes=db.goesMagDao().getBetween(0,200).single()
   assertEquals(null,goes.primary)
   assertEquals("GOES-19",goes.primaryLabel)
   assertEquals("GOES-18",goes.secondaryLabel)
   db.hemisphericPowerDao().upsertAll(listOf(HemisphericPowerEntity(200,20.0,null),HemisphericPowerEntity(100,10.0,11.0))); db.hemisphericPowerDao().deleteBefore(150)
   assertEquals(listOf(200L),db.hemisphericPowerDao().getBetween(0,300).map{it.timestampMillis})
  } finally { db.close() }
 }
}
