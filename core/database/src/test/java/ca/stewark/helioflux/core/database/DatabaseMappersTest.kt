package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMappersTest {
 @Test fun representativeModelsRoundTripWithoutLosingNullsOrTimestamps() {
  val models=listOf(
   SolarWindMag(101,null,2.0,null,4.0).let{it.toEntity().toDomain() to it},
   SolarWindPlasma(102,null,400.0,null).let{it.toEntity().toDomain() to it},
   KpSample(103,null).let{it.toEntity().toDomain() to it},
   GoesMagSample(104,null,5.0).let{it.toEntity().toDomain() to it},
   HemisphericPowerSample(105,20.0,null).let{it.toEntity().toDomain() to it},
   XrayFluxSample(106,null,2.0,null,4.0).let{it.toEntity().toDomain() to it},
   AceEpamSample(107,null,2.0,3.0,null,5.0).let{it.toEntity().toDomain() to it},
   FlareEvent("f","M1",108,"GOES",null,null).let{it.toEntity().toDomain() to it},
   CmeEvent("c",109,null,45.0,null,null,null).let{it.toEntity().toDomain() to it},
   ForecastSection("solar","Solar","summary","forecast",null).let{it.toEntity().toDomain() to it},
   SolarImage(SolarImageType.entries.first(),null,"url").let{it.toEntity().toDomain() to it},
   ActiveRegion("a",null,1.0,2.0).let{it.toEntity().toDomain() to it},
   EnlilFrame(110,111,"url").let{it.toEntity().toDomain() to it},
  ); models.forEach{(actual,expected)->assertEquals(expected,actual)}
  val aurora=AuroraSnapshot(112,113,listOf(AuroraPoint(50.0,-70.0,25.0))); val (s,p)=aurora.toEntities(); assertEquals(aurora,AuroraSnapshotWithPoints(s,p).toDomain())
 }
}
