package ca.stewark.helioflux.ui.solaractivity
import ca.stewark.helioflux.core.data.repository.RepositoryState;import ca.stewark.helioflux.core.model.*;import org.junit.Assert.*;import org.junit.Test
class SolarActivityComponentsTest{
@Test fun probabilityBadgesExposeCMXAndLoadingErrorStates(){val source=DataSourceKey("probability");assertEquals(listOf("C 60%","M 25%","X 5%"),flareProbabilityLabels(RepositoryState.Available(FlareProbabilities(60,25,5),source,DataFreshness.Fresh)));assertEquals(listOf("Loading flare probabilities"),flareProbabilityLabels(RepositoryState.Loading));assertEquals(listOf("Flare probabilities unavailable"),flareProbabilityLabels(RepositoryState.Failure(source,"down",null,null)))}
@Test fun imageryPresentationKeepsShellMetadataWhenImageUnavailable(){val p=solarImageryPresentation("LASCO C2","SOHO / LASCO",RepositoryState.Loading);assertEquals("LASCO C2",p.title);assertEquals("SOHO / LASCO",p.source);assertNull(p.imageUrl)}
@Test fun magnetogramCoordinatesMatchPwaMapping(){assertEquals(NormalizedRegionPosition(0.5,0.5),mapActiveRegion(0.0,0.0));assertEquals(NormalizedRegionPosition(1.0,0.0),mapActiveRegion(1000.0,1000.0));assertEquals(NormalizedRegionPosition(0.0,1.0),mapActiveRegion(-1000.0,-1000.0))}
}
