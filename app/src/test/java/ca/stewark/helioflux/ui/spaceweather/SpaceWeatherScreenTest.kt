package ca.stewark.helioflux.ui.spaceweather

import ca.stewark.helioflux.core.data.repository.RepositoryState
import ca.stewark.helioflux.core.model.*
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceWeatherScreenTest {
    @Test
    fun compactHierarchyMatchesApprovedDesign() {
        assertEquals(
            listOf(
                SpaceWeatherBlock.AuroraHero,
                SpaceWeatherBlock.SolarWindHeading,
                SpaceWeatherBlock.Metrics,
                SpaceWeatherBlock.Timeframe,
                SpaceWeatherBlock.BzBt,
                SpaceWeatherBlock.Density,
                SpaceWeatherBlock.Speed,
                SpaceWeatherBlock.Temperature,
                SpaceWeatherBlock.Goes,
                SpaceWeatherBlock.GeomagneticHeading,
                SpaceWeatherBlock.Kp,
                SpaceWeatherBlock.HemisphericPower,
            ),
            spaceWeatherBlocks(false).flatten(),
        )
    }

    @Test
    fun expandedRightPaneExcludesGlobeAndKeepsSingleColumnOrder() {
        val rows = spaceWeatherBlocks(true)
        val flattened = rows.flatten()

        assertFalse(flattened.contains(SpaceWeatherBlock.AuroraHero))
        assertEquals(
            listOf(
                SpaceWeatherBlock.SolarWindHeading,
                SpaceWeatherBlock.Metrics,
                SpaceWeatherBlock.Timeframe,
                SpaceWeatherBlock.BzBt,
                SpaceWeatherBlock.Density,
                SpaceWeatherBlock.Speed,
                SpaceWeatherBlock.Temperature,
                SpaceWeatherBlock.Goes,
                SpaceWeatherBlock.GeomagneticHeading,
                SpaceWeatherBlock.Kp,
                SpaceWeatherBlock.HemisphericPower,
            ),
            flattened,
        )
        assertTrue(rows.all { it.size == 1 })
    }

    @Test
    fun globeReceivesNormalizedSnapshotAndCachedFreshnessOnly() {
        val snapshot = AuroraSnapshot(1L, 2L, listOf(AuroraPoint(65.0, -50.0, 26.0)))
        val presentation = auroraGlobePresentation(
            RepositoryState.Available(
                snapshot,
                DataSourceKey("ovation"),
                DataFreshness.Cached,
            ),
        )

        assertEquals(snapshot.points, presentation.points)
        assertEquals(DataFreshness.Cached, presentation.freshness)
    }

    @Test
    fun unavailableAuroraDoesNotRemoveRestOfScreenData() {
        val presentation = auroraGlobePresentation(RepositoryState.Loading)
        assertTrue(presentation.points.isEmpty())
        assertEquals(null, presentation.freshness)
        assertTrue(
            spaceWeatherBlocks(false)
                .flatten()
                .contains(SpaceWeatherBlock.SolarWindHeading),
        )
    }

    @Test
    fun screenComputesOneSelectedDomainForAllCharts() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
        ).readText()

        assertEquals(
            1,
            Regex("""spaceWeatherChartDomain\(state\.timeframe,\s*nowMillis\)""")
                .findAll(source)
                .count(),
        )
        assertTrue(Regex("""val\s+chartDomain\s*=\s*spaceWeatherChartDomain""").containsMatchIn(source))
        assertTrue(source.contains("SpaceWeatherLineCard("))
        assertTrue(source.contains("KpChart("))
        assertTrue(source.contains("HemisphericPowerChart("))
    }

    @Test
    fun compactGlobeTouchStillPreventsLazyColumnFromCancellingSceneViewGesture() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
        ).readText()

        assertTrue(source.contains("CompactSpaceWeatherLayout"))
        assertTrue(source.contains("var globeTouchActive"))
        assertTrue(source.contains("userScrollEnabled = !globeTouchActive"))
        assertTrue(source.contains("{ globeTouchActive = it }"))
    }

    @Test
    fun expandedLayoutPinsGlobeLeftAndScrollsContentOnRight() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
        ).readText()

        assertTrue(source.contains("ExpandedSpaceWeatherLayout"))
        assertTrue(source.contains("ExpandedGlobeWeight"))
        assertTrue(source.contains("ExpandedContentWeight"))
        assertTrue(source.contains("spaceWeatherBlocks(true)"))
        assertTrue(source.contains("onGlobeTouchActiveChanged = {}"))
    }

    @Test
    fun compactAndExpandedLayoutsUseBlackScreenCanvas() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
        ).readText()

        val compact = source.substringAfter("private fun CompactSpaceWeatherLayout(").substringBefore("private fun ExpandedSpaceWeatherLayout(")
        val expanded = source.substringAfter("private fun ExpandedSpaceWeatherLayout(").substringBefore("@Composable\nprivate fun SpaceWeatherBlockList")

        assertTrue(compact.contains(".background(Color.Black)"))
        assertTrue(expanded.contains(".background(Color.Black)"))
    }

    @Test
    fun auroraHeroUsesBlackSquareStage() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
        ).readText()
        val hero = source
            .substringAfter("SpaceWeatherBlock.AuroraHero ->")
            .substringBefore("SpaceWeatherBlock.SolarWindHeading ->")

        assertTrue(Regex("""\.fillMaxWidth\(\)\s*\.aspectRatio\(1f\)""").containsMatchIn(hero))
        assertTrue(hero.contains(".background(Color.Black)"))
        assertTrue(hero.contains(".clip(RoundedCornerShape(16.dp))"))
        assertFalse(hero.contains(".height(340.dp)"))
    }
}
