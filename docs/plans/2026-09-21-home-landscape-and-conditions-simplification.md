# Home Landscape and Conditions Simplification Implementation Plan

**Date:** 2026-09-21  
**Design doc:** `docs/specs/2026-09-21-home-landscape-and-conditions-simplification-design.md`  
**Status:** Ready for review

## Overview

Simplify the Home current-conditions area to only the existing Kp, Bz, and Wind pills with 12dp vertical padding, and refactor Home layout behavior so portrait remains a single pull-to-refresh feed while landscape mirrors the proven Space Weather split-screen pattern: HELIOFLUX fixed across the top, solar hero fixed on the left, and an independently scrollable/pull-to-refresh right pane containing the pills and NOAA Forecast. Preserve all existing pill behavior, forecast-card behavior, hero animation/gestures, data flow, and shared heading styling.

Tasks 1–6 modify the same Home UI subsystem and should be implemented as one TDD batch. Run focused tests after each task, then run the full local verification gate once. Push one implementation batch to minimize GitHub Actions usage and monitor the resulting Android CI run to green.

## Tasks

### Task 1: Lock the simplified Current Conditions contract

**Files:**  
`app/src/test/java/ca/stewark/helioflux/ui/home/CurrentConditionsSourceTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeSectionHeadingSourceTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:**

Extend `CurrentConditionsSourceTest.kt` with a test that reads `CurrentConditions.kt` and asserts:

```kotlin
@Test
fun currentConditionsShowsOnlyPillsWithApprovedVerticalPadding() {
    val source = File(
        "src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt",
    ).readText()

    assertTrue(source.contains(".padding(vertical = 12.dp)"))
    assertFalse(source.contains("HelioFluxSectionHeading(\"Current Conditions\""))
    assertFalse(source.contains("Aurora / geomagnetic status unavailable"))
    assertFalse(source.contains("Geomagnetic storm conditions"))
    assertFalse(source.contains("Geomagnetic conditions below storm level"))
    assertTrue(source.contains("Metric(\"Kp\""))
    assertTrue(source.contains("Metric(\"Bz\""))
    assertTrue(source.contains("Metric(\"Wind\""))
}
```

Update `HomeSectionHeadingSourceTest.kt` so it no longer expects Current Conditions to use the shared heading. The revised test should assert:

```kotlin
assertFalse(current.contains("HelioFluxSectionHeading("))
assertTrue(
    forecast.contains(
        "HelioFluxSectionHeading(\"NOAA Forecast\", topSpacing = 0.dp)",
    ),
)
```

Add an instrumentation regression in `HomeDashboardTest.kt`:

```kotlin
@Test
fun currentConditionsRendersOnlyMetricPills() {
    compose.setContent { HomeScreen(HomeUiState(), false, {}) }

    compose.onNodeWithTag("condition-metrics").assertExists()
    compose.onNodeWithTag("metric-kp").assertExists()
    compose.onNodeWithTag("metric-bz").assertExists()
    compose.onNodeWithTag("metric-wind").assertExists()
    compose.onNodeWithText("Current Conditions").assertDoesNotExist()
    compose.onNodeWithText("Aurora / geomagnetic status unavailable").assertDoesNotExist()
    compose.onNodeWithText("Geomagnetic storm conditions").assertDoesNotExist()
    compose.onNodeWithText("Geomagnetic conditions below storm level").assertDoesNotExist()
}
```

Run the unit tests before implementation and confirm they fail against the current heading/status implementation.

**Implementation:**

Do not change production code in this task.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.CurrentConditionsSourceTest" --tests "ca.stewark.helioflux.ui.home.HomeSectionHeadingSourceTest"
```

The new expectations fail for the intended reasons.

---

### Task 2: Simplify CurrentConditions to the pill row only

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/CurrentConditionsSourceTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeSectionHeadingSourceTest.kt`

**Test first:**

Use the failing tests from Task 1. Do not add a second overlapping test before making this minimum implementation.

**Implementation:**

In `CurrentConditions.kt`:

1. Keep the `CurrentConditions` composable and `current-conditions` test tag.
2. Remove the outer `Column` if it is no longer needed for any content besides the row.
3. Render the existing metric `Row` directly with:
   ```kotlin
   Modifier
       .fillMaxWidth()
       .padding(vertical = 12.dp)
       .testTag("condition-metrics")
   ```
4. Keep:
   ```kotlin
   horizontalArrangement = Arrangement.spacedBy(6.dp)
   ```
5. Preserve the existing three `Metric` calls, their colors, values, `Modifier.weight(1f)`, and navigation callbacks exactly.
6. Remove:
   - `HelioFluxSectionHeading("Current Conditions", ...)`;
   - the 8dp spacer;
   - the local `kp` variable used only for status text;
   - the geomagnetic-status `Text`;
   - imports used only by the removed heading/status text, including `HelioFluxSectionHeading` and `SpaceMuted`.
7. Do not change the private `Metric` composable.

If retaining `current-conditions` on the outer row is cleaner, apply the tag to that row rather than introducing another wrapper.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.CurrentConditionsSourceTest" --tests "ca.stewark.helioflux.ui.home.HomeSectionHeadingSourceTest"
```

Both test classes pass.

---

### Task 3: Define the compact/expanded Home layout contract

**Files:**  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt` (new)  
`app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:**

Create `HomeScreenSourceTest.kt` with source-contract tests for the approved layout structure:

```kotlin
package ca.stewark.helioflux.ui.home

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenSourceTest {
    @Test
    fun expandedHomeUsesSpaceWeatherStyleSplitWeights() {
        assertEquals(0.43f, ExpandedHomeHeroWeight, 0.0f)
        assertEquals(0.57f, ExpandedHomeContentWeight, 0.0f)
    }

    @Test
    fun expandedMastheadAndHeroStayOutsideRightScrollPane() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt",
        ).readText()
        val expanded = source.substringAfter("private fun ExpandedHomeLayout(")
        val rightContent = source.substringAfter("private fun ExpandedHomeRightContent(")

        assertTrue(expanded.contains("HelioFluxMasthead()"))
        assertTrue(expanded.contains("home-expanded-hero"))
        assertTrue(expanded.contains("SolarHero("))
        assertTrue(expanded.contains("home-expanded-content"))
        assertTrue(expanded.contains("ExpandedHomeRightContent("))

        assertTrue(rightContent.contains("LazyColumn("))
        assertTrue(rightContent.contains("home-expanded-scroll"))
        assertTrue(rightContent.contains("CurrentConditions("))
        assertTrue(rightContent.contains("ForecastCards("))
        assertFalse(rightContent.contains("HelioFluxMasthead()"))
        assertFalse(rightContent.contains("SolarHero("))
    }
}
```

Add expanded-layout instrumentation expectations to `HomeDashboardTest.kt`:

```kotlin
@Test
fun expandedHomePinsHeroAndScrollsRightContent() {
    compose.setContent { HomeScreen(HomeUiState(), true, {}) }

    compose.onNodeWithTag("home-screen").assertExists()
    compose.onNodeWithTag("home-masthead").assertExists()
    compose.onNodeWithTag("home-expanded").assertExists()
    compose.onNodeWithTag("home-expanded-hero").assertExists()
    compose.onNodeWithTag("home-expanded-content").assertExists()
    compose.onNodeWithTag("home-expanded-scroll").assertExists()
    compose.onNodeWithTag("condition-metrics").assertExists()
    compose.onNodeWithTag("forecast-section").assertExists()
}
```

Update the existing `metricTapRoutesAndExpandedLayoutIsSideBySide` test name/expectations so it still verifies Kp routes to `SpaceWeather` but no longer describes the old hero/conditions-only side-by-side layout.

Run the new unit test before implementation and confirm it fails because the split constants/functions/tags do not yet exist.

**Implementation:**

Do not change production code in this task.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeScreenSourceTest"
```

It fails against the current single-`LazyColumn` Home implementation.

---

### Task 4: Extract the compact Home layout without changing portrait behavior

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:**

Extend `HomeDashboardTest.kt` so `homeRendersBrandedMastheadAndCompactComposition` also asserts:

```kotlin
compose.onNodeWithTag("condition-metrics").assertExists()
compose.onNodeWithTag("forecast-section").assertExists()
```

Keep the existing masthead, hero, and `home-compact` assertions. This protects the required compact order/content while the screen is split into layout-specific functions.

**Implementation:**

In `HomeScreen.kt`:

1. Keep forecast extraction in `HomeScreen`.
2. Change `HomeScreen` to dispatch explicitly:
   ```kotlin
   if (expanded) {
       ExpandedHomeLayout(...)
   } else {
       CompactHomeLayout(...)
   }
   ```
3. Add:
   ```kotlin
   @Composable
   private fun CompactHomeLayout(...)
   ```
4. Move the existing portrait `PullToRefreshBox` into `CompactHomeLayout`.
5. Preserve the existing black canvas and `home-pull-refresh` tag.
6. Preserve a vertical `LazyColumn` tagged `home-screen`, with:
   - horizontal 16dp padding;
   - existing 12dp spacing between the masthead item and the compact content item.
7. Keep `HelioFluxMasthead()` as the first item.
8. In the second item render a `Column` tagged `home-compact` **without** its own `Arrangement.spacedBy(12.dp)`:
   ```text
   SolarHero
   CurrentConditions
   ForecastCards
   ```
   This intentionally lets `CurrentConditions` own the exact 12dp gap above and below the pills, avoiding doubled 24dp spacing.
9. Keep `ForecastCards(expandedLayout = false)`.
10. Preserve the existing `Modifier.fillMaxWidth().padding(bottom = 24.dp)` on the forecast section.
11. Do not change `HelioFluxMasthead`, `SolarHero`, or `ForecastCards` internals.

At the end of this task, `ExpandedHomeLayout` may remain a placeholder that reproduces the current expanded behavior only if needed to keep the source compiling; the final expanded structure is completed in Task 6. Prefer implementing the final function signatures now and filling the expanded body in Task 6 rather than adding temporary abstractions.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.CurrentConditionsSourceTest" --tests "ca.stewark.helioflux.ui.home.HomeSectionHeadingSourceTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

Compact Home and instrumentation sources compile, and the CurrentConditions contracts remain green.

---

### Task 5: Add the expanded Home split-layout constants and structure

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt`

**Test first:**

Use the failing `HomeScreenSourceTest` from Task 3. It must still be red before the expanded implementation.

**Implementation:**

In `HomeScreen.kt`, add:

```kotlin
internal const val ExpandedHomeHeroWeight = 0.43f
internal const val ExpandedHomeContentWeight = 0.57f
```

Add:

```kotlin
@Composable
private fun ExpandedHomeLayout(...)

@Composable
private fun ExpandedHomeRightContent(...)
```

Structure `ExpandedHomeLayout` as:

```text
Column (black, fillMaxSize, home-screen)
├── HelioFluxMasthead()
└── Row (weight 1f, fillMaxWidth, home-expanded)
    ├── fixed hero Box (43%, fillMaxHeight, home-expanded-hero)
    └── PullToRefreshBox (57%, fillMaxHeight, home-expanded-content)
        └── ExpandedHomeRightContent
```

Specific requirements:

1. Root `Column`:
   - `fillMaxSize()`;
   - black background;
   - `testTag("home-screen")`.
2. `HelioFluxMasthead()` is a direct root child before the split row. It must not be inside `PullToRefreshBox` or `LazyColumn`.
3. Split `Row`:
   - `Modifier.fillMaxWidth().weight(1f).testTag("home-expanded")`.
4. Left hero box:
   - `weight(ExpandedHomeHeroWeight)`;
   - `fillMaxHeight()`;
   - padding modeled on Space Weather: start 16dp, top 12dp, end 8dp, bottom 12dp;
   - `testTag("home-expanded-hero")`;
   - `contentAlignment = Alignment.TopCenter`;
   - existing `SolarHero(state.frames, Modifier.fillMaxWidth())`.
5. Right `PullToRefreshBox`:
   - same `isRefreshing` and `onRefresh` supplied to HomeScreen;
   - `weight(ExpandedHomeContentWeight)`;
   - `fillMaxHeight()`;
   - `testTag("home-expanded-content")`.
6. Do not place a second pull-to-refresh surface around the left hero or masthead.
7. Do not change hero state/playback/gesture code.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeScreenSourceTest"
```

The split-weight assertions pass. The source-structure test may remain partially red until Task 6 adds the final right-content function.

---

### Task 6: Build the independently scrollable right content pane

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:**

Before completing the right pane, ensure the source-contract test from Task 3 is still failing on the missing `ExpandedHomeRightContent` details.

Keep the instrumentation test `expandedHomePinsHeroAndScrollsRightContent` ready to compile against these tags:

```text
home-expanded-content
home-expanded-scroll
condition-metrics
forecast-section
```

**Implementation:**

Implement `ExpandedHomeRightContent` as one vertical `LazyColumn`:

```kotlin
LazyColumn(
    modifier =
        Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 16.dp)
            .testTag("home-expanded-scroll"),
) {
    item {
        CurrentConditions(
            state.conditions,
            onDestination,
            Modifier.fillMaxWidth(),
        )
    }
    item {
        ForecastCards(
            sections = forecasts,
            expandedLayout = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        )
    }
}
```

Do **not** add `verticalArrangement = Arrangement.spacedBy(...)` between these items. The simplified `CurrentConditions` owns the exact 12dp padding above and below the pills, so NOAA Forecast follows after that approved 12dp lower breathing room rather than receiving an extra stacked gap.

The right pane must:
- scroll independently of the fixed left hero;
- contain CurrentConditions before ForecastCards;
- keep NOAA Forecast's existing shared heading;
- preserve the existing horizontal `LazyRow` forecast browsing and card expansion behavior;
- use the same Home data and callbacks already available in `HomeScreen`;
- introduce no new state.

Update `HomeDashboardTest.kt` as planned in Task 3 and retain the existing Kp navigation assertion.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeScreenSourceTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

All new source-contract tests pass and the instrumentation suite compiles.

---

### Task 7: Run focused Home regression coverage

**Files:** No planned production changes. If a regression is found, add the narrowest failing test before the minimum fix.

**Test first:**

No new test is added solely for this gate.

**Implementation / verification:**

Run the Home-focused unit tests:

```bash
./gradlew :app:testDebugUnitTest   --tests "ca.stewark.helioflux.ui.home.CurrentConditionsSourceTest"   --tests "ca.stewark.helioflux.ui.home.HomeSectionHeadingSourceTest"   --tests "ca.stewark.helioflux.ui.home.HomeScreenSourceTest"
```

Then compile instrumentation tests:

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

If a connected/emulator device is available locally, also run the Home instrumentation class:

```bash
./gradlew :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.helioflux.ui.home.HomeDashboardTest
```

Verify existing Home behaviors still covered by `HomeDashboardTest`:
- cached hero state;
- loading hero;
- masthead;
- metric navigation;
- three condition pills;
- forecast expansion/collapse;
- empty/default state.

**Verify:**

Focused unit tests pass, instrumentation sources compile, and connected Home tests pass when a device is available.

---

### Task 8: Run the full local verification gate

**Files:** No planned source changes.

**Test first:**

No new test solely for this verification task. Any failure discovered here must be reproduced with the narrowest relevant failing test before changing code.

**Implementation / verification:**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Then inspect the diff:

```bash
git status --short
git diff --stat
```

Expected planned files are limited to:

```text
app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt
app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt
app/src/test/java/ca/stewark/helioflux/ui/home/CurrentConditionsSourceTest.kt
app/src/test/java/ca/stewark/helioflux/ui/home/HomeSectionHeadingSourceTest.kt
app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt
app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt
```

No SolarHero, ForecastCards, shared heading, Space Weather, repository, ViewModel, network, or model files should be modified.

**Verify:**

All Gradle commands pass and the diff contains only planned Home files.

---

### Task 9: Push one Home implementation batch and monitor Android CI

**Files:** No additional planned source changes.

**Test first:**

No new test is added for the push itself. If CI exposes a defect, reproduce it with the narrowest relevant local test before changing code.

**Implementation / verification:**

1. Commit Tasks 1–6 together after Tasks 7–8 are green.
2. Suggested commit message:
   ```text
   ui: simplify Home conditions and split landscape layout
   ```
3. Push once to `main`.
4. Actively monitor the resulting Android CI run during the same implementation session.
5. Re-check roughly every 45 seconds while pending.
6. If CI fails:
   - inspect the failed job/log;
   - reproduce the failure with a focused test where possible;
   - make the minimum correction;
   - push the correction;
   - continue monitoring until green.
7. Do not begin unrelated UI work while CI is pending.

**Verify:**

Android CI completes with `conclusion: success` for the final implementation SHA.

---

### Task 10: Perform the physical Pixel acceptance pass

**Files:** No planned source changes. A discovered defect starts the project's `$debugprompt` workflow before code changes.

**Test first:**

No automated test is added solely for manual acceptance.

**Implementation / verification:**

On the physical Pixel:

1. In portrait, confirm the visible order is:
   ```text
   HELIOFLUX
   Solar hero
   Kp / Bz / Wind
   NOAA Forecast
   ```
2. Confirm `Current Conditions` is absent.
3. Confirm all geomagnetic-status text beneath the pills is absent.
4. Confirm the pills have approximately 12dp breathing room above and below without looking like a 24dp doubled gap.
5. Confirm all three pills retain their existing values, colors, dimensions, and tap navigation to Space Weather.
6. Rotate to landscape.
7. Confirm HELIOFLUX stays fixed at the top while the right pane scrolls.
8. Confirm the solar hero stays fixed in the left pane.
9. Confirm the right pane scrolls independently through the pills and NOAA Forecast.
10. Expand a long forecast card and confirm the expanded content remains reachable by scrolling the right pane.
11. Confirm pull-to-refresh works from the right pane while the masthead and hero remain stationary.
12. Confirm hero animation, pinch/pan, double-tap reset, loading state, and cached/offline message are unchanged.
13. Rotate back to portrait and confirm the compact layout returns correctly with no clipped or duplicated content.

If an on-device defect appears, stop this feature workflow and run `.github/agents/debugprompt.agent.md` for that defect.

**Verify:**

The physical-device checklist passes with no unintended Home or hero regression.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production-code change was preceded by focused failing coverage
- [ ] `Current Conditions` is no longer rendered on Home
- [ ] Geomagnetic-status text is no longer rendered beneath the Home pills
- [ ] Kp, Bz, and Wind pills remain otherwise unchanged
- [ ] The pill row owns exactly 12dp vertical padding above and below
- [ ] Portrait remains HELIOFLUX -> hero -> pills -> NOAA Forecast
- [ ] Compact Home retains whole-screen pull-to-refresh behavior
- [ ] Landscape HELIOFLUX masthead remains fixed above both panes
- [ ] Landscape solar hero remains fixed in the left pane
- [ ] Landscape right pane owns vertical scrolling and pull-to-refresh
- [ ] Landscape uses 43% / 57% hero/content proportions
- [ ] Right pane order is CurrentConditions -> NOAA Forecast
- [ ] NOAA Forecast still uses the existing shared section heading
- [ ] Forecast cards retain horizontal browsing and expand/collapse behavior
- [ ] Solar hero animation/loading/gesture code is unchanged
- [ ] No Space Weather, repository, ViewModel, network, model, or shared-heading behavior changed
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:compileDebugAndroidTestKotlin` passes
- [ ] `./gradlew :app:lintDebug :app:assembleDebug` passes
- [ ] No unplanned files are modified
- [ ] Final Android CI is green
- [ ] Physical Pixel acceptance passes
