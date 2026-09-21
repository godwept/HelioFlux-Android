# Section Heading Standardization and Geomagnetic GOES Layout Implementation Plan

**Date:** 2026-09-21  
**Design doc:** `docs/specs/2026-09-21-section-heading-standardization-design.md`  
**Status:** Ready for review

## Overview

Create one reusable HelioFlux section-heading composable and apply it to the Home and Space Weather screens without redesigning surrounding cards or charts. The shared heading will use the normal UI font at 20sp SemiBold with slight tracking, white/on-background text, a restrained Solar Orange vertical accent, and standardized section spacing. Space Weather will also move GOES Magnetometer beneath Hemispheric Power in the Geomagnetic Activity section and decouple GOES from the Solar Wind timeframe selector by giving it a fixed two-day series window and chart domain. Existing chart rendering, refresh routing, repository/network behavior, Aurora globe behavior, and non-GOES data behavior remain unchanged.

## Implementation batch

Tasks 1–4 modify the same UI/presentation subsystem and can remain atomic. Follow TDD for each task, run the focused test immediately after its minimum implementation, then run the full local verification once after Task 4. Commit/push Tasks 1–4 together as one implementation batch and monitor Android CI to green. Do not create extra implementation commits unless a failing test or CI run requires a focused correction.

## Tasks

### Task 1: Add the reusable HelioFlux section heading

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeading.kt` (new)  
`app/src/test/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeadingTest.kt` (new)

**Test first:**

Create `HelioFluxSectionHeadingTest.kt` with focused unit/source-contract tests for the approved visual constants:

```kotlin
package ca.stewark.helioflux.ui.components

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelioFluxSectionHeadingTest {
    @Test
    fun headingUsesApprovedTypographyAndSpacing() {
        assertEquals(20.sp, SectionHeadingFontSize)
        assertEquals(FontWeight.SemiBold, SectionHeadingFontWeight)
        assertEquals(0.25.sp, SectionHeadingLetterSpacing)
        assertEquals(20.dp, SectionHeadingTopSpacing)
        assertEquals(8.dp, SectionHeadingBottomSpacing)
        assertEquals(3.dp, SectionHeadingAccentWidth)
        assertEquals(20.dp, SectionHeadingAccentHeight)
    }

    @Test
    fun headingUsesThemeSolarAccentWithoutDecorativeEffects() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeading.kt",
        ).readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.primary"))
        assertTrue(source.contains("MaterialTheme.colorScheme.onBackground"))
        assertTrue(source.contains("RoundedCornerShape"))
    }
}
```

Run the test before implementation and confirm it fails because the new component/constants do not exist yet.

**Implementation:**

Create `HelioFluxSectionHeading.kt`.

Define these module-visible constants so the visual contract is directly testable:

```kotlin
internal val SectionHeadingFontSize = 20.sp
internal val SectionHeadingFontWeight = FontWeight.SemiBold
internal val SectionHeadingLetterSpacing = 0.25.sp
internal val SectionHeadingTopSpacing = 20.dp
internal val SectionHeadingBottomSpacing = 8.dp
internal val SectionHeadingAccentWidth = 3.dp
internal val SectionHeadingAccentHeight = 20.dp
```

Add:

```kotlin
@Composable
fun HelioFluxSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    topSpacing: Dp = SectionHeadingTopSpacing,
)
```

Implement it as one left-aligned `Row` that:

1. Applies `topSpacing` and `SectionHeadingBottomSpacing` through the outer modifier.
2. Vertically centers the accent and text.
3. Uses approximately 8dp horizontal space between accent and title.
4. Renders a narrow `Box` using:
   - width `SectionHeadingAccentWidth`;
   - height `SectionHeadingAccentHeight`;
   - mildly rounded ends via `RoundedCornerShape`;
   - `MaterialTheme.colorScheme.primary`, which is the app's Solar Orange theme accent.
5. Renders the supplied title unchanged using the normal Material typography font family with:
   - `SectionHeadingFontSize`;
   - `SectionHeadingFontWeight`;
   - `SectionHeadingLetterSpacing`;
   - `MaterialTheme.colorScheme.onBackground`.
6. Does not uppercase the title and does not add glow, shadow, card/background surface, or horizontal divider.

The `topSpacing` parameter exists only to suppress redundant top padding when the heading is already first in its containing section. Do not add other style variants.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.HelioFluxSectionHeadingTest"
```

The new test passes.

---

### Task 2: Replace Home headings with the shared component

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeSectionHeadingSourceTest.kt` (new)

**Test first:**

Create `HomeSectionHeadingSourceTest.kt`:

```kotlin
package ca.stewark.helioflux.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionHeadingSourceTest {
    @Test
    fun homeSectionsUseSharedHeadingWithoutDuplicateTopSpacing() {
        val current = File(
            "src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt",
        ).readText()
        val forecast = File(
            "src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt",
        ).readText()

        assertTrue(
            current.contains(
                "HelioFluxSectionHeading(\"Current Conditions\", topSpacing = 0.dp)",
            ),
        )
        assertTrue(
            forecast.contains(
                "HelioFluxSectionHeading(\"NOAA Forecast\", topSpacing = 0.dp)",
            ),
        )
        assertFalse(current.contains("Text(\"Current Conditions\""))
        assertFalse(forecast.contains("Text(\"NOAA Forecast\""))
    }
}
```

Run it first and confirm it fails against the existing raw `Text` headings.

**Implementation:**

In `CurrentConditions.kt`:

1. Import `HelioFluxSectionHeading`.
2. Replace the existing `Text("Current Conditions", ...)` with:
   ```kotlin
   HelioFluxSectionHeading("Current Conditions", topSpacing = 0.dp)
   ```
3. Remove the outer `Column`'s `verticalArrangement = Arrangement.spacedBy(8.dp)` so the heading's approved 8dp bottom spacing is not doubled.
4. Preserve the existing 8dp gap between the metric row and geomagnetic-status text explicitly with a single `Spacer(Modifier.height(8.dp))` after the metric row.
5. Do not change metric pill dimensions, colors, click behavior, or status text.

In `ForecastCards.kt`:

1. Import `HelioFluxSectionHeading`.
2. Replace the existing `Text("NOAA Forecast", ...)` with:
   ```kotlin
   HelioFluxSectionHeading("NOAA Forecast", topSpacing = 0.dp)
   ```
3. Remove the section `Column`'s `Arrangement.spacedBy(10.dp)`; the heading's 8dp bottom spacing becomes the single approved heading-to-content gap.
4. Leave the `LazyRow`, card width/height, internal card spacing, expansion behavior, and forecast styling untouched.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeSectionHeadingSourceTest" --tests "ca.stewark.helioflux.ui.home.CurrentConditionsSourceTest"
```

Both tests pass.

---

### Task 3: Apply the shared heading and regroup GOES under Geomagnetic Activity

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`

**Test first:**

Update the existing compact and expanded hierarchy expectations so GOES is after Hemispheric Power:

```kotlin
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
            SpaceWeatherBlock.GeomagneticHeading,
            SpaceWeatherBlock.Kp,
            SpaceWeatherBlock.HemisphericPower,
            SpaceWeatherBlock.Goes,
        ),
        spaceWeatherBlocks(false).flatten(),
    )
}
```

Make the same order change in `expandedRightPaneExcludesGlobeAndKeepsSingleColumnOrder`.

Add spacing and shared-heading contract coverage:

```kotlin
@Test
fun sectionBoundariesUseApprovedSpacingWithoutChangingNormalCardSpacing() {
    assertEquals(0.dp, spaceWeatherBlockSpacingBefore(null, SpaceWeatherBlock.AuroraHero))
    assertEquals(
        0.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.AuroraHero,
            SpaceWeatherBlock.SolarWindHeading,
        ),
    )
    assertEquals(
        0.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.SolarWindHeading,
            SpaceWeatherBlock.Metrics,
        ),
    )
    assertEquals(
        14.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.Metrics,
            SpaceWeatherBlock.Timeframe,
        ),
    )
    assertEquals(
        0.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.Temperature,
            SpaceWeatherBlock.GeomagneticHeading,
        ),
    )
    assertEquals(
        0.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.GeomagneticHeading,
            SpaceWeatherBlock.Kp,
        ),
    )
    assertEquals(
        14.dp,
        spaceWeatherBlockSpacingBefore(
            SpaceWeatherBlock.HemisphericPower,
            SpaceWeatherBlock.Goes,
        ),
    )
}

@Test
fun spaceWeatherUsesSharedSectionHeading() {
    val source = File(
        "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
    ).readText()

    assertTrue(source.contains("HelioFluxSectionHeading(\"Solar Wind\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"Geomagnetic Activity\""))
    assertFalse(source.contains("private fun SectionHeading("))
}
```

Add the required `dp` import to the test file.

Run these tests first and confirm the old ordering/private heading fail them.

**Implementation:**

In `SpaceWeatherScreen.kt`:

1. Reorder `OrderedSpaceWeatherBlocks` to:
   ```text
   AuroraHero
   SolarWindHeading
   Metrics
   Timeframe
   BzBt
   Density
   Speed
   Temperature
   GeomagneticHeading
   Kp
   HemisphericPower
   Goes
   ```
2. Keep `SpaceWeatherBlock.Goes` and its refresh routing unchanged.
3. Replace the private `SectionHeading` composable with `HelioFluxSectionHeading` for both section-heading blocks, then delete the private function.
4. Add:
   ```kotlin
   internal val SpaceWeatherDefaultBlockSpacing = 14.dp

   internal fun spaceWeatherBlockSpacingBefore(
       previous: SpaceWeatherBlock?,
       current: SpaceWeatherBlock,
   ): Dp = when {
       previous == null -> 0.dp
       previous == SpaceWeatherBlock.SolarWindHeading ||
           previous == SpaceWeatherBlock.GeomagneticHeading -> 0.dp
       current == SpaceWeatherBlock.SolarWindHeading ||
           current == SpaceWeatherBlock.GeomagneticHeading -> 0.dp
       else -> SpaceWeatherDefaultBlockSpacing
   }
   ```
5. Remove `verticalArrangement = Arrangement.spacedBy(14.dp)` from `SpaceWeatherBlockList`.
6. Iterate with `rows.forEachIndexed`. Before each block, add a `Spacer` only when `spaceWeatherBlockSpacingBefore(previous, block)` is non-zero.
7. Add a `headingTopSpacing: Dp` argument to `SpaceWeatherBlockContent`. Pass:
   - `0.dp` when the block is the first row in the right/compact list;
   - `SectionHeadingTopSpacing` otherwise.
8. For `SolarWindHeading` and `GeomagneticHeading`, pass that value to `HelioFluxSectionHeading(..., topSpacing = headingTopSpacing)`.
9. Keep the direct expanded-layout `AuroraHero` call working by using a default `headingTopSpacing = SectionHeadingTopSpacing` in `SpaceWeatherBlockContent`; the value is irrelevant for the Aurora block.
10. Do not alter globe placement, pull-to-refresh, chart/card composables, or refresh-source mapping.

This preserves the existing 14dp spacing between ordinary cards, gives 20dp before non-first headings, gives 8dp after headings through the shared component, and suppresses top padding when Solar Wind is first in the expanded right pane.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherScreenTest"
```

All Space Weather screen tests pass.

---

### Task 4: Decouple GOES from the Solar Wind timeframe and fix it to two days

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`

**Test first:**

In `SpaceWeatherChartsTest.kt`, replace the current GOES timeframe-dependent calls and add fixed-window coverage:

```kotlin
@Test
fun goesUsesFixedTwoDayWindow() {
    val tooOld = GoesMagSample(
        now - Timeframe.TwoDays.durationMillis - 1,
        1.0,
        2.0,
    )
    val recent = GoesMagSample(now - 1_000, 3.0, 4.0)
    val goes = GoesMagnetometerSeries(listOf(tooOld, recent), "P", "S")

    val out = goesSeries(goes, now)

    assertEquals(
        listOf(recent.timestampMillis.toDouble()),
        out[0].points.map { it.x },
    )
    assertEquals(
        listOf(recent.timestampMillis.toDouble()),
        out[1].points.map { it.x },
    )
}

@Test
fun goesDomainIsAlwaysTwoDays() {
    assertEquals(
        ChartDomain(
            minX = (now - Timeframe.TwoDays.durationMillis).toDouble(),
            maxX = now.toDouble(),
        ),
        goesChartDomain(now),
    )
}
```

Update `goesKeepsDynamicLabelsAndNullGaps` to call `goesSeries(goes, now)`.

Update `missingGoesDoesNotFabricateSeries` to:

```kotlin
assertTrue(goesSeriesOrEmpty(null, now).isEmpty())
```

In `SpaceWeatherScreenTest.kt`, replace the old `screenComputesOneSelectedDomainForAllCharts` contract with a scope-specific test:

```kotlin
@Test
fun selectedDomainIsSolarWindOnlyAndGoesUsesFixedDomain() {
    val source = File(
        "src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt",
    ).readText()

    assertTrue(
        Regex("""val\s+chartDomain\s*=\s*spaceWeatherChartDomain\(state\.timeframe,\s*nowMillis\)""")
            .containsMatchIn(source),
    )

    val goesBlock = source
        .substringAfter("SpaceWeatherBlock.Goes ->")
        .substringBefore("SpaceWeatherBlock.GeomagneticHeading ->")

    assertTrue(goesBlock.contains("goesSeriesOrEmpty(goes, nowMillis)"))
    assertTrue(goesBlock.contains("goesChartDomain(nowMillis)"))
    assertFalse(goesBlock.contains("state.timeframe"))
}
```

Because Task 3 moves GOES after Hemispheric Power, make the substring end at the end of the GOES branch (for example the closing branch before the `when` ends) rather than relying on the old Geomagnetic-heading position. The test must isolate only the GOES branch and assert that it contains no `state.timeframe` reference.

Run the focused tests before implementation and confirm the old GOES signature/behavior fails.

**Implementation:**

In `SpaceWeatherCharts.kt`:

1. Add:
   ```kotlin
   fun goesChartDomain(now: Long): ChartDomain =
       chartDomain(now, Timeframe.TwoDays.durationMillis)
   ```
2. Change:
   ```kotlin
   fun goesSeries(
       series: GoesMagnetometerSeries,
       now: Long,
   ): List<LineChartSeries>
   ```
3. Inside `goesSeries`, keep the existing labels/styles/null-gap mapping but always filter with:
   ```kotlin
   filterByTimeframe(
       series.samples,
       Timeframe.TwoDays,
       now,
   ) { it.timestampMillis }
   ```
4. Change:
   ```kotlin
   fun goesSeriesOrEmpty(
       series: GoesMagnetometerSeries?,
       now: Long,
   ): List<LineChartSeries> =
       series?.let { goesSeries(it, now) }.orEmpty()
   ```
5. Do not change `bzBtSeries`, `plasmaSeries`, `spaceWeatherChartDomain`, Kp, or Hemispheric Power behavior.

In the `SpaceWeatherBlock.Goes` branch of `SpaceWeatherScreen.kt`:

1. Replace:
   ```kotlin
   goesSeriesOrEmpty(goes, state.timeframe, nowMillis)
   ```
   with:
   ```kotlin
   goesSeriesOrEmpty(goes, nowMillis)
   ```
2. Replace the Solar Wind `chartDomain` argument with:
   ```kotlin
   goesChartDomain(nowMillis)
   ```
3. Leave `onRefresh`, `refreshing`, `refreshContentDescription`, metadata, and series styles untouched.

The selected `state.timeframe` continues to control only Bz/Bt, Density, Speed, and Temperature.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest" --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherScreenTest"
```

Both test classes pass.

---

### Task 5: Run the full local regression suite

**Files:** No planned source changes. If a regression is found, add a focused failing test before the minimum fix.

**Test first:**

No new test is added solely for this verification task. Any failure uncovered here must be treated as a regression and fixed test-first.

**Implementation / verification:**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
```

Then verify the diff is narrowly scoped:

```bash
git status --short
git diff --stat
```

Expected planned files are limited to:

```text
app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeading.kt
app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt
app/src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt
app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt
app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt
app/src/test/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeadingTest.kt
app/src/test/java/ca/stewark/helioflux/ui/home/HomeSectionHeadingSourceTest.kt
app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt
app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt
```

No ViewModel, repository, network, chart renderer, Aurora globe, or theme file should be modified.

**Verify:**

Both Gradle commands pass and `git diff --stat` contains only planned files.

---

### Task 6: Push one atomic implementation batch and monitor Android CI

**Files:** No additional planned source changes.

**Test first:**

No new test is added for the push itself. If CI exposes a defect, reproduce it with the narrowest relevant failing local test before changing code.

**Implementation / verification:**

1. Commit Tasks 1–4 together after Task 5 is green. Suggested commit message:
   ```text
   ui: standardize section headings and regroup GOES
   ```
2. Push once to `main`.
3. Actively monitor the resulting Android CI run during the same implementation session.
4. While pending, re-check the run roughly every 45 seconds.
5. If CI fails:
   - inspect the failed job/log;
   - add or run the focused regression test that reproduces the failure;
   - make the minimum correction;
   - push the correction;
   - continue monitoring until green.
6. Do not begin unrelated UI work while CI is pending.

**Verify:**

Android CI completes with `conclusion: success` for the final implementation SHA.

---

### Task 7: Perform the approved physical-device acceptance check

**Files:** No planned source changes. A discovered defect starts the project's `$debugprompt` workflow before code changes.

**Test first:**

No new automated test is added solely for manual verification. Any defect found on-device requires a focused failing regression test before its fix.

**Implementation / verification:**

On the physical Pixel:

1. Open Home and confirm `Current Conditions` and `NOAA Forecast` use the same 20sp SemiBold heading treatment with the narrow Solar Orange vertical accent.
2. Confirm the Home headings remain visually subordinate to the HELIOFLUX masthead and that metric/forecast cards themselves are unchanged.
3. Open Space Weather in portrait and confirm `Solar Wind` and `Geomagnetic Activity` use the exact same shared heading treatment as Home.
4. Confirm the layout order is:
   ```text
   Solar Wind
   metrics
   timeframe selector
   Bz/Bt
   Density
   Speed
   Temperature
   Geomagnetic Activity
   Kp
   Hemispheric Power
   GOES Magnetometer
   ```
5. Confirm the heading-to-first-content gap is compact and consistent, the gap before a new section is clearly larger, and ordinary chart-to-chart spacing remains unchanged.
6. Rotate/use the expanded layout and confirm Solar Wind does not gain redundant top padding when it is first in the right pane.
7. Cycle `1h -> 3h -> 12h -> 2d`; Bz/Bt, Density, Speed, and Temperature continue to update/refit.
8. While cycling those buttons, confirm GOES Magnetometer remains on the same fixed two-day window.
9. Confirm Kp and Hemispheric Power behavior is unchanged.
10. Refresh GOES from its existing chart refresh control and confirm the existing refresh behavior still works.
11. Scroll through Space Weather and interact with the existing charts/globe to confirm this change did not reintroduce scrolling, chart-composition, or globe-lifecycle regressions.

If any on-device defect appears, stop this feature workflow and run `.github/agents/debugprompt.agent.md` for that defect.

**Verify:**

The physical-device checklist passes with no unintended visual or behavioral regression.

## Definition of Done

- [ ] Tasks 1–7 completed in order
- [ ] Every production-code change was preceded by a focused failing test
- [ ] `HelioFluxSectionHeading` is the single reusable heading used by the four approved Home/Space Weather headings
- [ ] Heading typography is 20sp SemiBold with 0.25sp tracking, title case, normal UI font, and on-background text
- [ ] Heading accent is a restrained 3dp × 20dp Solar Orange/theme-primary vertical bar with rounded ends
- [ ] Headings have 20dp section-top spacing where appropriate and 8dp heading-to-content spacing without double padding
- [ ] `Current Conditions` and `NOAA Forecast` use the shared heading without changing their cards/content behavior
- [ ] `Solar Wind` and `Geomagnetic Activity` use the shared heading in compact and expanded Space Weather layouts
- [ ] Ordinary Space Weather card-to-card spacing remains 14dp
- [ ] GOES Magnetometer is ordered after Hemispheric Power inside Geomagnetic Activity
- [ ] Solar Wind `1h / 3h / 12h / 2d` selection affects only Bz/Bt, Density, Speed, and Temperature
- [ ] GOES Magnetometer always uses the most recent two days and a fixed two-day chart domain
- [ ] GOES refresh routing, metadata, labels, series styles, loading behavior, and chart interactions remain unchanged
- [ ] Kp and Hemispheric Power behavior is unchanged
- [ ] No metric card, forecast card, chart renderer, repository/network, ViewModel, Aurora globe, or HELIOFLUX masthead redesign is included
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:lintDebug :app:assembleDebug` passes
- [ ] No unplanned files are modified
- [ ] The single pushed implementation batch reaches green Android CI
- [ ] Physical-device acceptance passes
