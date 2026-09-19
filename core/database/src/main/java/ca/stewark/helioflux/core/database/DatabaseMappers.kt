package ca.stewark.helioflux.core.database

import ca.stewark.helioflux.core.database.dao.AuroraSnapshotWithPoints
import ca.stewark.helioflux.core.database.entity.*
import ca.stewark.helioflux.core.model.*

fun SolarWindMag.toEntity()=SolarWindMagEntity(timestampMillis,bx,by,bz,bt); fun SolarWindMagEntity.toDomain()=SolarWindMag(timestampMillis,bx,by,bz,bt)
fun SolarWindPlasma.toEntity()=SolarWindPlasmaEntity(timestampMillis,density,speed,temperature); fun SolarWindPlasmaEntity.toDomain()=SolarWindPlasma(timestampMillis,density,speed,temperature)
fun KpSample.toEntity()=KpEntity(timestampMillis,kp); fun KpEntity.toDomain()=KpSample(timestampMillis,kp)
fun GoesMagSample.toEntity()=GoesMagEntity(timestampMillis,primary,secondary); fun GoesMagEntity.toDomain()=GoesMagSample(timestampMillis,primary,secondary)
fun HemisphericPowerSample.toEntity()=HemisphericPowerEntity(timestampMillis,north,south); fun HemisphericPowerEntity.toDomain()=HemisphericPowerSample(timestampMillis,north,south)
fun XrayFluxSample.toEntity()=XrayFluxEntity(timestampMillis,goes18Short,goes18Long,goes19Short,goes19Long); fun XrayFluxEntity.toDomain()=XrayFluxSample(timestampMillis,goes18Short,goes18Long,goes19Short,goes19Long)
fun AceEpamSample.toEntity()=AceEpamEntity(timestampMillis,electronLow,electronHigh,protonLow,protonMid,protonHigh); fun AceEpamEntity.toDomain()=AceEpamSample(timestampMillis,electronLow,electronHigh,protonLow,protonMid,protonHigh)
fun FlareEvent.toEntity()=FlareEventEntity(id,flareClass,timestampMillis,observatory,region,location); fun FlareEventEntity.toDomain()=FlareEvent(id,flareClass,timestampMillis,observatory,region,location)
fun CmeEvent.toEntity()=CmeEventEntity(id,timestampMillis,speed,halfAngle,direction,type,link); fun CmeEventEntity.toDomain()=CmeEvent(id,timestampMillis,speed,halfAngle,direction,type,link)
fun ForecastSection.toEntity()=ForecastSectionEntity(key,title,summary,forecast,issueTime); fun ForecastSectionEntity.toDomain()=ForecastSection(key,title,summary,forecast,issueTime)
fun SolarImage.toEntity()=SolarImageEntity(type,sourceTimestampMillis,url); fun SolarImageEntity.toDomain()=SolarImage(type,sourceTimestampMillis,url)
fun ActiveRegion.toEntity()=ActiveRegionEntity(id,number,helioprojectiveX,helioprojectiveY); fun ActiveRegionEntity.toDomain()=ActiveRegion(id,number,helioprojectiveX,helioprojectiveY)
fun EnlilFrame.toEntity()=EnlilFrameEntity(runTimestampMillis,frameTimestampMillis,url); fun EnlilFrameEntity.toDomain()=EnlilFrame(runTimestampMillis,frameTimestampMillis,url)
fun AuroraSnapshot.toEntities()=AuroraSnapshotEntity(observationTimestampMillis,forecastTimestampMillis) to points.map{AuroraPointEntity(observationTimestampMillis,it.latitude,it.longitude,it.intensity)}
fun AuroraSnapshotWithPoints.toDomain()=AuroraSnapshot(snapshot.observationTimestampMillis,snapshot.forecastTimestampMillis,points.map{AuroraPoint(it.latitude,it.longitude,it.intensity)})
