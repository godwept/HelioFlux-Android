# Solar Activity Chart Spacing and Flare Probability Placement Implementation Plan

**Date:** 2026-09-21  
**Design doc:** `docs/specs/2026-09-21-solar-activity-chart-spacing-probability-design.md`  
**Status:** Ready for review

## Overview

Remove the large idle blank bands above Solar Activity charts by changing the shared Vico inspection marker from the default top-reserved placement to `AroundPoint`, while preserving all existing marker content and chart behavior. Move the existing C/M/X flare probability pills out of the top-level compact and expanded Solar Activity feeds and into the X-Ray Activity section directly below its heading and above the chart/state content. Do not change chart heights, domains, axes, guides, series, reduction, data flow, imagery, event cards, repositories, or ViewModels.

## Tasks

### Task 1: Lock the floating marker placement contract

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`

**Test first:**

Extend `ChartPresentationTest` with a source-level contract for the shared Vico marker:

```kotlin
@Test
fun markerInspectionFloatsAroundSelectedPoint() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt")
            .readText()

    assertTrue(
        source.contains(
            "labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint",
        ),
    )
    assertTrue(source.contains("lineCount = chartMarkerLineCount(preparedSeries.size)"))
    assertTrue(source.contains("valueFormatter = markerFormatter"))
    assertTrue(source.contains("guideline ="))
}
```

Run the focused test and confirm it fails because `rememberDefaultCartesianMarker` currently relies on Vico's default top placement.

**Implementation:**

In `HelioFluxLineChart.kt`, add exactly one marker-presentation argument to the existing `rememberDefaultCartesianMarker` call:

```kotlin
labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
```

Keep the existing `DefaultCartesianMarker` import; no new marker type is needed.

Do not change:

- `markerFormatter`;
- `chartMarkerLineCount`;
- marker text colors/font sizes;
- guideline styling;
- `CartesianMarkerController.rememberShowOnPress`;
- `consumeMoveEvents`;
- chart scroll/zoom policy;
- axes, decorations, domains, series, or reduction.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```

All chart-presentation tests pass, including the existing timestamp, line-count, interaction-policy, reduction, and marker-row tests.

---

### Task 2: Lock the X-Ray probability placement contract

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Extend `SolarActivityScreenSourceTest` with a contract that requires the X-Ray section to receive probability state and render the existing badge component before the X-Ray state/chart content:

```kotlin
@Test
fun xraySectionOwnsFlareProbabilityBadges() {
    assertTrue(
        source.contains(
            "probabilities: RepositoryState<FlareProbabilities>",
        ),
    )
    assertTrue(source.contains("FlareProbabilityBadges(probabilities)"))

    val xraySectionStart = source.indexOf("private fun XrayActivitySection(")
    val heading = source.indexOf(
        "HelioFluxSectionHeading(\"X-Ray Activity\", topSpacing = 0.dp)",
        xraySectionStart,
    )
    val badges = source.indexOf("FlareProbabilityBadges(probabilities)", xraySectionStart)
    val chart = source.indexOf("XraySection(state, chartDomain)", xraySectionStart)

    assertTrue(heading < badges)
    assertTrue(badges < chart)
}
```

Run the focused test and confirm it fails because `XrayActivitySection` currently accepts only X-Ray state and chart domain.

**Implementation:**

In `SolarActivityScreen.kt`:

1. Change the X-Ray item inside `solarActivityScienceItems` from:
   ```kotlin
   XrayActivitySection(state.xray, chartDomain)
   ```
   to:
   ```kotlin
   XrayActivitySection(
       state = state.xray,
       probabilities = state.probabilities,
       chartDomain = chartDomain,
   )
   ```

2. Change `XrayActivitySection` to accept:
   ```kotlin
   probabilities: RepositoryState<FlareProbabilities>,
   ```

3. Keep the section as a single `Column`, ordered:
   ```kotlin
   HelioFluxSectionHeading("X-Ray Activity", topSpacing = 0.dp)
   FlareProbabilityBadges(probabilities)
   XraySection(state, chartDomain)
   ```

4. Do not modify `FlareProbabilityBadges.kt`.

Do not add a new wrapper, duplicate probability presentation logic, or transform the `RepositoryState`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

The new placement contract passes.

---

### Task 3: Remove the two top-level probability rows

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Add a source-level test that proves the probability component is invoked exactly once in `SolarActivityScreen.kt`:

```kotlin
@Test
fun flareProbabilityBadgesAreRenderedOnlyInsideXraySection() {
    assertEquals(
        1,
        Regex("FlareProbabilityBadges\\(").findAll(source).count(),
    )
}
```

Run the focused test and confirm it fails because the current screen renders the badge component in compact, expanded, and X-Ray locations after Task 2.

**Implementation:**

Remove these two standalone lazy-list items from `SolarActivityScreen.kt`:

Compact feed:

```kotlin
item {
    FlareProbabilityBadges(state.probabilities)
}
```

Expanded science feed:

```kotlin
item {
    FlareProbabilityBadges(state.probabilities)
}
```

Keep:

- the existing status-bar inset modifiers;
- the freshness indicator;
- Solar Imagery positioning;
- `solarActivityScienceItems`;
- all other lazy-item ordering.

After this change, `FlareProbabilityBadges` should appear exactly once in the source: inside `XrayActivitySection`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

Confirm the invocation-count test passes and the existing compact/expanded layout contracts remain green.

---

### Task 4: Update mixed-state instrumentation coverage for the new probability location

**Files:**  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`

**Test first:**

Update `mixedSectionStatesDoNotBecomeScreenWideError` so it no longer assumes the `25%` probability value is immediately visible at the top of the screen.

After confirming Solar Imagery remains visible, scroll the compact feed to the probability strip:

```kotlin
rule.onNodeWithTag("solar-activity-compact")
    .performScrollToNode(hasTestTag("flare-probability-strip"))

rule.onNodeWithTag("flare-probability-strip").assertExists()
rule.onNodeWithText("25%").assertIsDisplayed()
```

Keep the existing later scroll/assertions for:

- `recent-events-row`;
- cached flare state;
- empty CME state;
- retained flare row.

This test should fail against the old top-level expectation if changed before production placement and pass once Tasks 2–3 are complete.

**Implementation:**

No production change should be needed in this task.

Do not add new test tags: reuse the existing `flare-probability-strip` tag from `FlareProbabilityBadges.kt`.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

Instrumentation sources compile.

---

### Task 5: Add compact and expanded placement coverage

**Files:**  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Add probability data to a reusable Solar Activity test state:

```kotlin
probabilities =
    RepositoryState.Available(
        FlareProbabilities(25, 5, 1),
        source,
        DataFreshness.Fresh,
    ),
```

Then add two focused tests.

Compact:

```kotlin
@Test
fun compactXraySectionContainsProbabilityStrip() {
    compose.setContent {
        SolarActivityScreen(populatedScienceState(), expanded = false)
    }

    compose.onNodeWithTag("solar-activity-compact")
        .performScrollToNode(hasText("X-Ray Activity"))

    compose.onNodeWithText("X-Ray Activity").assertExists()
    compose.onNodeWithTag("flare-probability-strip").assertExists()
    compose.onNodeWithText("25%").assertExists()
}
```

Expanded:

```kotlin
@Test
fun expandedXraySectionContainsProbabilityStrip() {
    compose.setContent {
        SolarActivityScreen(populatedScienceState(), expanded = true)
    }

    compose.onNodeWithTag("solar-activity-expanded-scroll")
        .performScrollToNode(hasText("X-Ray Activity"))

    compose.onNodeWithText("X-Ray Activity").assertExists()
    compose.onNodeWithTag("flare-probability-strip").assertExists()
    compose.onNodeWithText("25%").assertExists()
}
```

Use an existing state helper if it already contains suitable probability data; otherwise add the smallest helper/copy needed. Do not add X-Ray sample data solely for this placement test unless required by Compose visibility.

**Implementation:**

No further production behavior should be required.

If the test reveals duplicated probability strips, correct the screen composition rather than changing the badge component.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

Both new instrumentation tests compile.

---

### Task 6: Run focused chart and Solar Activity regressions

**Files:** No production changes expected.

**Test first / verification gate:**

Run the focused unit suites:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
```

Then compile instrumentation coverage:

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

**Implementation:**

Fix only failures caused by the approved marker-placement or probability-placement changes.

Do not alter:

- chart height;
- X-Ray 72-hour domain;
- X-Ray C/M/X reference lines;
- ACE EPAM series;
- chart navigation policy;
- flare/CME event cards;
- imagery/fullscreen/ENLIL code;
- repository/ViewModel behavior.

**Verify:**

All focused unit tests pass and instrumentation tests compile.

---

### Task 7: Run the full local verification gate and inspect scope

**Files:** No production changes expected.

**Test first / verification gate:**

Run exactly:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Inspect the final diff before pushing.

**Expected production files:**

- `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Expected test files:**

- `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`
- `app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

Do not modify:

- `FlareProbabilityBadges.kt`;
- `XrayChart.kt`;
- `AceEpamChart.kt`;
- chart domain/reduction helpers;
- flare/CME lists;
- imagery/fullscreen/ENLIL files;
- ViewModel/repository/network/data files;
- Home or Space Weather screen code.

**Verify:**

All commands pass and the diff contains only planned files.

---

### Task 8: Push one implementation batch and monitor Android CI

**Files:** Git history only.

**Test first / pre-push gate:**

Do not push until Task 7 is fully green.

**Implementation:**

1. Commit the complete implementation as one focused batch to minimize GitHub Actions usage.
2. Suggested commit message:
   `ui: tighten Solar Activity chart spacing`
3. Push to `main` once.
4. Monitor the resulting Android CI run during the same response.
5. Re-check roughly every 45 seconds while pending.
6. If CI fails:
   - inspect the failing step/logs;
   - identify the root cause;
   - make the minimum correction;
   - push the fix;
   - continue monitoring until green.
7. Do not start unrelated work while CI is pending.

**Verify:**

Android CI is green through:

- unit tests;
- instrumentation-test compilation;
- connected instrumentation step when a device is available;
- lint;
- debug APK assembly;
- artifact upload.

If the runner has no connected Android device, explicitly report that connected instrumentation execution remains pending.

---

### Task 9: Perform the physical Pixel acceptance pass

**Files:** No code changes unless a new defect is found.

**Test first / physical verification:**

On the Pixel, verify:

1. C/M/X probability pills no longer appear at the top of Solar Activity.
2. X-Ray Activity is visually ordered:
   - heading;
   - C/M/X probability pills;
   - chart/state content.
3. The large idle blank band above the X-Ray plot is gone.
4. The large idle blank band above the Particle Environment plot is gone.
5. Press/drag inspection still works on X-Ray.
6. Press/drag inspection still works on Particle Environment.
7. The tooltip floats near the selected data point rather than occupying a fixed top strip.
8. Tooltip timestamp and all series values remain readable.
9. X-Ray axes, C/M/X reference lines, 72-hour domain, colors, and gestures remain unchanged.
10. Particle Environment axes, series, and gestures remain unchanged.
11. Solar Imagery, flare/CME cards, fullscreen imagery, ENLIL, refresh, and landscape split remain unchanged.

**Implementation:**

If the floating tooltip itself causes a new clipping/overlap issue, or if a substantial gap remains after marker placement changes, stop and route that distinct physical-device defect through `$debugprompt` before changing code.

**Verify:**

The Pixel acceptance checklist passes.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Shared chart marker uses `DefaultCartesianMarker.LabelPosition.AroundPoint`.
- [ ] Marker formatter, line count, guideline, and press/drag behavior are preserved.
- [ ] X-Ray and Particle charts no longer reserve the large idle top marker band.
- [ ] Flare probability badges render only once in `SolarActivityScreen.kt`.
- [ ] C/M/X pills appear below X-Ray Activity and above its chart/state content.
- [ ] Top-level compact/expanded probability rows are removed.
- [ ] `FlareProbabilityBadges.kt` remains unchanged.
- [ ] Chart heights, domains, axes, references, series, and reduction remain unchanged.
- [ ] All new behavior has tests written before implementation.
- [ ] Focused unit tests pass.
- [ ] Instrumentation tests compile.
- [ ] Full unit suite, lint, and debug APK build pass.
- [ ] No unrelated files are modified.
- [ ] One focused implementation batch is pushed.
- [ ] Android CI is green.
- [ ] Physical Pixel acceptance passes.
