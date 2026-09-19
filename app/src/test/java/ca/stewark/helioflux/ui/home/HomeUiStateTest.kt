package ca.stewark.helioflux.ui.home
import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
class HomeUiStateTest{private val source=DataSourceKey("test");@Test fun conditionsUseLatestUsableCachedValues(){val s=HomeUiState(magnetic=RepositoryState.Available(listOf(SolarWindMag(1,null,null,-4.2,null)),source,DataFreshness.Fresh),plasma=RepositoryState.Available(listOf(SolarWindPlasma(1,null,455.0,null)),source,DataFreshness.Fresh),kp=RepositoryState.Available(listOf(KpSample(1,5.0)),source,DataFreshness.Fresh),flare=RepositoryState.Available(FlareProbabilities(30,12,2),source,DataFreshness.Fresh));assertEquals(-4.2,s.conditions.bz);assertEquals(455.0,s.conditions.speed);assertEquals(5.0,s.conditions.kp);assertEquals(12,s.conditions.flare?.m)}}
